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
import scala.reflect.ClassTag
import scala.language.implicitConversions
import org.apache.mnemonic.NonVolatileMemAllocator
import org.apache.mnemonic.DurableType
import org.apache.mnemonic.EntityFactoryProxy
import org.apache.mnemonic.sessions.ObjectCreator
import org.apache.commons.io.FileUtils

class DurableRDDFunctions[T: ClassTag](rdd: RDD[T]) extends Serializable {

  def makeDurable[D: ClassTag] (
      serviceName: String,
      durableTypes: Array[DurableType],
      entityFactoryProxies: Array[EntityFactoryProxy],
      slotKeyId: Long,
      partitionPoolSize: Long,
      f: (T, ObjectCreator[D, NonVolatileMemAllocator]) => Option[D],
      preservesPartitioning: Boolean = false) = {
    DurableRDD[D, T](rdd,
      serviceName, durableTypes, entityFactoryProxies, slotKeyId,
      partitionPoolSize, f, preservesPartitioning)
  }

  def saveAsMnemonic[D: ClassTag] (path: String, cleanPath: Boolean,
                     serviceName: String,
                     durableTypes: Array[DurableType],
                     entityFactoryProxies: Array[EntityFactoryProxy],
                     slotKeyId: Long,
                     partitionPoolSize: Long,
                     f: (T, ObjectCreator[D, NonVolatileMemAllocator]) => Option[D]) {
    val dir = new File(path)
    if (cleanPath && dir.exists) {
      FileUtils.deleteDirectory(dir)
    }
    if (!dir.exists) {
      dir.mkdir
    }
    val cleanF = f // rdd.context.clean(f)
    val func = DurableRDD.prepareDurablePartition[D, T] (path,
      serviceName, durableTypes, entityFactoryProxies, slotKeyId,
      partitionPoolSize, cleanF)_
    rdd.context.runJob(rdd, func)
  }
}

object DurableRDDFunctions {
  implicit def addDurableRDDFunctions[T: ClassTag](rdd: RDD[T]) = new DurableRDDFunctions[T](rdd)
}
