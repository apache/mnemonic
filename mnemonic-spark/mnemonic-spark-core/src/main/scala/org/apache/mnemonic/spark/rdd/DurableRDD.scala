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
import java.nio.file.Path

import org.apache.spark.rdd.RDD
import org.apache.spark.{ Partition, TaskContext }
import scala.reflect.{ classTag, ClassTag }
import scala.collection.mutable.HashMap
import scala.collection.JavaConverters._
import org.apache.mnemonic.ConfigurationException;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.NonVolatileMemAllocator;
import org.apache.mnemonic.sessions.DurableInputSession;
import org.apache.mnemonic.sessions.SessionIterator;
import org.apache.mnemonic.sessions.ObjectCreator
import org.apache.mnemonic.spark.MneDurableInputSession
import org.apache.mnemonic.spark.MneDurableOutputSession

class DurableRDD[D: ClassTag, T: ClassTag](
  var prev: RDD[T],
  serviceName: String, durableTypes: Array[DurableType],
  entityFactoryProxies: Array[EntityFactoryProxy], slotKeyId: Long,
  partitionPoolSize: Long, baseDir: Path,
  f: (T, ObjectCreator[D]) => Option[D],
  preservesPartitioning: Boolean = false)
    extends RDD[D](prev) {

  private val _parmap = HashMap.empty[Partition, Array[File]]

  override val partitioner = if (preservesPartitioning) firstParent[T].partitioner else None

  override protected def getPartitions: Array[Partition] = firstParent[T].partitions

  def prepareDurablePartition(split: Partition, context: TaskContext,
      iterator: Iterator[T]) {
    val outsess = MneDurableOutputSession[D](serviceName,
        durableTypes, entityFactoryProxies, slotKeyId,
        partitionPoolSize, baseDir,
        f"mem_${this.hashCode()}%10d_${split.hashCode()}%10d")
    try {
      for (item <- iterator) {
        f(item, outsess) match {
          case Some(res) => outsess.post(res)
          case None =>
        }
      }
      _parmap += (split -> outsess.fileList)
    } finally {
      outsess.close()
    }
  }

  override def compute(split: Partition, context: TaskContext): Iterator[D] = {
    if (!(_parmap contains split)) {
      prepareDurablePartition(split, context, firstParent[T].iterator(split, context))
    }
    val flist = _parmap.get(split) match {
      case Some(flst) => flst
      case None => throw new RuntimeException("Not construct durable partition properly")
    }
    val insess = MneDurableInputSession[D](serviceName,
        durableTypes, entityFactoryProxies, slotKeyId, flist)
    insess.iterator.asScala
  }

  override def clearDependencies() {
    super.clearDependencies()
    prev = null
  }

}
