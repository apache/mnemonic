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

package org.apache.mnemonic.spark.rdd;

import java.io.File
import scala.util._

import org.apache.spark.rdd.RDD
import org.apache.spark.{ Partition, TaskContext, SparkContext }
import org.apache.spark.internal.Logging
import org.apache.commons.io.FileUtils
import scala.reflect.{ classTag, ClassTag }
import scala.collection.mutable.HashMap
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import org.apache.mnemonic.ConfigurationException
import org.apache.mnemonic.DurableType
import org.apache.mnemonic.EntityFactoryProxy
import org.apache.mnemonic.NonVolatileMemAllocator
import org.apache.mnemonic.sessions.DurableInputSession
import org.apache.mnemonic.sessions.SessionIterator
import org.apache.mnemonic.sessions.ObjectCreator
import org.apache.mnemonic.spark.MneDurableInputSession
import org.apache.mnemonic.spark.MneDurableOutputSession
import org.apache.mnemonic.spark.DurableException

private[spark] class DurableRDD[D: ClassTag, T: ClassTag] (
  private var rdd: RDD[T],
  serviceName: String, durableTypes: Array[DurableType],
  entityFactoryProxies: Array[EntityFactoryProxy], slotKeyId: Long,
  partitionPoolSize: Long, durableDirectory: String,
  f: (T, ObjectCreator[D, NonVolatileMemAllocator]) => Option[D],
  preservesPartitioning: Boolean = false)
    extends RDD[D](rdd) {

  val durdddir = DurableRDD.getRddDirName(durableDirectory, id)
  DurableRDD.resetRddDir(durdddir)

  override val partitioner = if (preservesPartitioning) firstParent[T].partitioner else None

  override protected def getPartitions: Array[Partition] = firstParent[T].partitions

  def prepareDurablePartition(split: Partition, context: TaskContext,
      iterator: Iterator[T]): Array[File] = {
    val outsess = MneDurableOutputSession[D](serviceName,
        durableTypes, entityFactoryProxies, slotKeyId,
        partitionPoolSize, durdddir.toString,
        DurableRDD.genDurableFileName(split.hashCode)_)
    try {
      for (item <- iterator) {
        f(item, outsess) match {
          case Some(res) => outsess.post(res)
          case None =>
        }
      }
    } finally {
      outsess.close()
    }
    outsess.memPools.toArray
  }

  override def compute(split: Partition, context: TaskContext): Iterator[D] = {
    val mempListOpt: Option[Array[File]] =
      DurableRDD.collectMemPoolFileList(durdddir.toString, DurableRDD.genDurableFileName(split.hashCode)_)
    val memplist = mempListOpt match {
      case None => {
        val mplst = prepareDurablePartition(split, context, firstParent[T].iterator(split, context))
        logInfo(s"Done transformed RDD #${rdd.id} to durableRDD #${id} on ${durdddir.toString}")
        mplst
      }
      case Some(mplst) => mplst
    }
    val insess = MneDurableInputSession[D](serviceName,
        durableTypes, entityFactoryProxies, slotKeyId, memplist)
    insess.iterator.asScala
  }

  override def clearDependencies {
    super.clearDependencies()
    rdd = null
  }

  def reset {
    DurableRDD.resetRddDir(durdddir)
  }
}

object DurableRDD {

  val durableSubDirNameTemplate = "durable-rdd-%010d"
  val durableFileNameTemplate = "mem_%010d_%010d.mne"

  private var durableDir: Option[String] = None

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
    val durdddir = new File(rddDirName)
    if (durdddir.exists) {
      FileUtils.deleteDirectory(durdddir)
    }
    if (!durdddir.mkdir) {
      throw new DurableException(s"Durable RDD directory ${durdddir.toString} cannot be created")
    }
  }

  def genDurableFileName(splitId: Int)(mempidx: Long): String = {
    durableFileNameTemplate.format(splitId, mempidx)
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

  def apply[D: ClassTag, T: ClassTag] (
      rdd: RDD[T],
      serviceName: String, durableTypes: Array[DurableType],
      entityFactoryProxies: Array[EntityFactoryProxy], slotKeyId: Long,
      partitionPoolSize: Long,
      f: (T, ObjectCreator[D, NonVolatileMemAllocator]) => Option[D],
      preservesPartitioning: Boolean = false) = {
    val sc: SparkContext = rdd.context
    val ret = new DurableRDD[D, T](rdd,
      serviceName, durableTypes, entityFactoryProxies, slotKeyId,
      partitionPoolSize, getDurableDir(sc).get, f, preservesPartitioning)
    //sc.cleaner.foreach(_.registerRDDForCleanup(ret))
    ret
  }

  def cleanupForApp(sc: SparkContext) {
    FileUtils.deleteDirectory(new File(getDurableDir(sc).get))
  }

  def cleanupForRdd(sc: SparkContext, rddid: Int) {
    FileUtils.deleteDirectory(new File(getRddDirName(getDurableDir(sc).get, rddid)))
  }
}
