/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mnemonic.spark.rdd

import java.io.File

import org.apache.spark.rdd.RDD
import org.apache.spark._
import org.apache.commons.io.FileUtils

import scala.reflect.ClassTag
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import org.apache.mnemonic.{ConfigurationException, DurableType, EntityFactoryProxy, NonVolatileMemAllocator}
import org.apache.mnemonic.sessions.ObjectCreator
import org.apache.mnemonic.spark.MneDurableInputSession
import org.apache.mnemonic.spark.MneDurableOutputSession
import org.apache.mnemonic.spark.DurableException

import scala.collection.mutable

private[spark] class DurableRDD[D: ClassTag, T: ClassTag] (
  @transient private var _sc: SparkContext,
  @transient private var deps: Seq[Dependency[_]],
  serviceName: String, durableTypes: Array[DurableType],
  entityFactoryProxies: Array[EntityFactoryProxy], slotKeyId: Long,
  partitionPoolSize: Long, durableDirectory: String,
  f: (T, ObjectCreator[D, NonVolatileMemAllocator]) => Option[D],
  preservesPartitioning: Boolean = false)
    extends RDD[D](_sc, deps) {

  private var inSess: MneDurableInputSession[D, _] = null
  private val isInputOnly =  Nil == deps

  private var durdddir:String = _
  if (isInputOnly) {
    val dir = new File(durableDirectory)
    if (!dir.exists) {
      throw new ConfigurationException("Input directory does not exist")
    }
    durdddir = durableDirectory
  } else {
    durdddir = DurableRDD.getRddDirName(durableDirectory, id)
    DurableRDD.resetRddDir(durdddir)
  }

  override val partitioner = if (!isInputOnly && preservesPartitioning) firstParent[T].partitioner else None

  override protected def getPartitions: Array[Partition] = {
    if (isInputOnly) {
      val ret = DurableRDD.collectMemPoolPartitionList(durdddir).getOrElse(Array[Partition]())
      if (ret.isEmpty) {
        logWarning(s"Not found any partitions in the directory ${durdddir}")
      } else {
        logInfo(s"Found ${ret.length} partitions in the directory ${durdddir}")
      }
      ret
    } else {
      firstParent[T].partitions
    }
  }

  override def compute(split: Partition, context: TaskContext): InterruptibleIterator[D] = {
    context.addTaskCompletionListener { context =>
      logInfo(s"Task of #${context.partitionId} completion")
      close
    }

    val mempoollistgen = ()=>DurableRDD.collectMemPoolFileList(durdddir,
                                        DurableRDD.genDurableFileName(context.partitionId)_)
    val insess = if (isInputOnly) {
      logInfo(s"Input only #${context.partitionId}")
      updateInputSession(MneDurableInputSession[D](
        serviceName, durableTypes, entityFactoryProxies, slotKeyId, mempoollistgen ))
    } else {
      logInfo(s"Starting transform RDD #${firstParent[T].id}_${split.index} to durableRDD #${id} on ${durdddir}")
      val outsess = MneDurableOutputSession[D](serviceName,
        durableTypes, entityFactoryProxies, slotKeyId,
        partitionPoolSize, durdddir,
        DurableRDD.genDurableFileName(context.partitionId)_)
      val ins = MneDurableInputSession[D, T](serviceName, durableTypes, entityFactoryProxies,
        slotKeyId, mempoollistgen,
        outsess, firstParent[T].iterator(split, context), f,
        new File(durdddir, DurableRDD.durablePartClaimFileNameTemplate.format(context.partitionId)))
      //insess.transformAhead
      updateInputSession(ins)
    }
    new InterruptibleIterator(context, insess.iterator.asScala)
  }

  def updateInputSession(insess: MneDurableInputSession[D, _]): MneDurableInputSession[D, _] = {
    if (null != inSess) {
      inSess.close
    }
    inSess = insess
    inSess
  }

  def close: Unit = {
    updateInputSession(null)
  }

  override def clearDependencies {
    super.clearDependencies()
  }

  def reset {
    DurableRDD.resetRddDir(durdddir)
  }

  def destroy {
    DurableRDD.deleteRddDir(durdddir)
  }
}

object DurableRDD {

  val durableSubDirNameTemplate = "durable-rdd-%010d"
  val durableFileNameTemplate = "mem_%010d_%010d.mne"
  val durablePartClaimFileNameTemplate = "claim_%010d"
  val durableFileNamePartitionRegex = raw".*mem_(\d{10})_0000000000.mne".r

  private var durableDir: Option[String] = None

  def genDurableFileName(splitId: Long)(mempidx: Long): String = {
    durableFileNameTemplate.format(splitId, mempidx)
  }

  def getDefaultDurableBaseDir(sc: SparkContext): String = {
    try {
      sc.getConf.get("spark.durable-basedir").trim
    } catch {
      case _: NoSuchElementException =>
        throw new IllegalArgumentException("spark.durable-basedir not specified for DurableRDD")
      case _: Throwable =>
        throw new DurableException("transforming durable base directories failed")
    }
  }

  def setDurableBaseDir(sc: SparkContext, baseDir: String) {
    durableDir = Option(baseDir) map { bdir =>
      val fdir = new File(bdir, sc.applicationId)
      if (!fdir.exists && !fdir.mkdirs) {
        throw new DurableException(s"Cannot create durable directory ${fdir.toString}")
      }
      fdir.toString
    }
  }

  def getDurableDir(sc: SparkContext): Option[String] = {
    durableDir match {
      case None => setDurableBaseDir(sc, getDefaultDurableBaseDir(sc))
      case _ =>
    }
    durableDir
  }

  def getRddDirName(durableDir: String, rddid: Int): String = {
    new File(durableDir, durableSubDirNameTemplate.format(rddid)).toString
  }

  def resetRddDir(rddDirName: String) {
    deleteRddDir(rddDirName)
    createRddDir(rddDirName)
  }

  def createRddDir(rddDirName: String) {
    val dir = new File(rddDirName)
    if (!dir.mkdir) {
      throw new DurableException(s"Durable RDD directory ${dir.toString} cannot be created")
    }
  }

  def deleteRddDir(rddDirName: String) {
    val dir = new File(rddDirName)
    if (dir.exists) {
      FileUtils.deleteDirectory(dir)
    }
  }

  def collectMemPoolPartitionList(path: String): Option[Array[Partition]] = {
    val paridset = new mutable.TreeSet[Int]
    val dir = new File(path)
    if (dir.isDirectory) {
      val flst = dir.listFiles.filter(!_.isDirectory)
      for (file <- flst) {
        file.toString match {
          case durableFileNamePartitionRegex(paridx) => {
            paridset += paridx.toInt
          }
          case _ =>
        }
      }
    }
    Option(paridset.toArray.map(x => new Partition with Serializable { override def index:Int = x }))
  }

  def collectMemPoolFileList(durddir: String, memFileNameGen: (Long)=>String): Option[Array[File]] = {
    val flist: ArrayBuffer[File] = new ArrayBuffer[File]
    var idx: Long = 0L
    var file: File = null
    var wstop = true
    while (wstop) {
      file = new File(durddir, memFileNameGen(idx))
      idx = idx + 1
      if (file.exists) {
        flist += file
      } else {
        wstop = false
      }
    }
    if (flist.isEmpty) {
      None
    } else {
      Some(flist.toArray)
    }
  }

  def prepareDurablePartition[D: ClassTag, T: ClassTag] (path: String,
                              serviceName: String, durableTypes: Array[DurableType],
                              entityFactoryProxies: Array[EntityFactoryProxy], slotKeyId: Long,
                              partitionPoolSize: Long,
                              func: (T, ObjectCreator[D, NonVolatileMemAllocator]) => Option[D]
                             ) (context: TaskContext, iterator: Iterator[T]): Array[File] = {
    val outsess = MneDurableOutputSession[D](serviceName,
      durableTypes, entityFactoryProxies, slotKeyId,
      partitionPoolSize, path,
      genDurableFileName(context.partitionId)_)
    try {
      for (item <- iterator) {
        func(item, outsess) match {
          case Some(res) => outsess.post(res)
          case None =>
        }
      }
    } finally {
      outsess.close()
    }
    outsess.memPools.toArray
  }

  def apply[D: ClassTag, T: ClassTag] (
      rdd: RDD[T],
      serviceName: String, durableTypes: Array[DurableType],
      entityFactoryProxies: Array[EntityFactoryProxy], slotKeyId: Long,
      partitionPoolSize: Long,
      f: (T, ObjectCreator[D, NonVolatileMemAllocator]) => Option[D],
      preservesPartitioning: Boolean = false) = {
    val sc: SparkContext = rdd.context
    val cleanF = f // sc.clean(f)
    val ret = new DurableRDD[D, T](rdd.context , List(new OneToOneDependency(rdd)),
      serviceName, durableTypes, entityFactoryProxies, slotKeyId,
      partitionPoolSize, getDurableDir(sc).get, cleanF, preservesPartitioning)
    //sc.cleaner.foreach(_.registerRDDForCleanup(ret))
    ret
  }

  def apply[D: ClassTag] (
      sc: SparkContext, path: String,
      serviceName: String, durableTypes: Array[DurableType],
      entityFactoryProxies: Array[EntityFactoryProxy], slotKeyId: Long) = {
    val ret = new DurableRDD[D, Unit](sc, Nil,
      serviceName, durableTypes, entityFactoryProxies, slotKeyId,
      1024*1024*1024L, path, null)
    ret
  }

  def cleanupForApp(sc: SparkContext) {
    FileUtils.deleteDirectory(new File(getDurableDir(sc).get))
  }

  def cleanupForRdd(sc: SparkContext, rddid: Int) {
    FileUtils.deleteDirectory(new File(getRddDirName(getDurableDir(sc).get, rddid)))
  }
}
