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

package org.apache.mnemonic.spark

import java.io.File
import scala.reflect.{ classTag, ClassTag }
import scala.collection.mutable.ArrayBuffer

import org.apache.mnemonic.ConfigurationException
import org.apache.mnemonic.DurableType
import org.apache.mnemonic.EntityFactoryProxy
import org.apache.mnemonic.NonVolatileMemAllocator
import org.apache.mnemonic.Utils
import org.apache.mnemonic.collections.DurableSinglyLinkedList
import org.apache.mnemonic.collections.DurableSinglyLinkedListFactory
import org.apache.mnemonic.sessions.DurableOutputSession

private[spark] class MneDurableOutputSession[V: ClassTag] (
    serviceName: String,
    durableTypes: Array[DurableType],
    entityFactoryProxies: Array[EntityFactoryProxy],
    slotKeyId: Long,
    partitionPoolSize: Long,
    durableDirectory: String,
    outputMemFileNameGen: (Long)=>String)
    extends DurableOutputSession[V, NonVolatileMemAllocator] {

  val memPools: ArrayBuffer[File] = new ArrayBuffer[File]
  private var _outidx: Long = 0L

  initialize(serviceName, durableTypes, entityFactoryProxies,
      slotKeyId, partitionPoolSize, durableDirectory, outputMemFileNameGen)

  def initialize(
    serviceName: String,
    durableTypes: Array[DurableType],
    entityFactoryProxies: Array[EntityFactoryProxy],
    slotKeyId: Long,
    partitionPoolSize: Long,
    durableDirectory: String,
    outputMemFileNameGen: (Long)=>String) {
    setServiceName(serviceName)
    setDurableTypes(durableTypes)
    setEntityFactoryProxies(entityFactoryProxies)
    setSlotKeyId(slotKeyId)
    setPoolSize(partitionPoolSize)
    if (!initNextPool) {
      throw new DurableException("Firstly init next pool failed")
    }
  }

  protected def genNextPoolFile(): File = {
    val file = new File(durableDirectory, outputMemFileNameGen(_outidx))
    _outidx += 1
    memPools += file
    file
  }

  override def initNextPool(): Boolean = {
    var ret: Boolean = false
    if (null != getAllocator) {
      getAllocator.close()
      setAllocator(null)
    }
    val outputFile = genNextPoolFile
    if (outputFile.exists) {
      throw new DurableException(s"Durable memory file already exists ${outputFile}")
    }
    m_act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService(getServiceName),
      getPoolSize, outputFile.toString, true);
    if (null != getAllocator) {
      m_newpool = true;
      ret = true
    }
    ret
  }

}

object MneDurableOutputSession {
  def apply[V: ClassTag](
    serviceName: String,
    durableTypes: Array[DurableType],
    entityFactoryProxies: Array[EntityFactoryProxy],
    slotKeyId: Long,
    partitionPoolSize: Long,
    durableDirectory: String,
    outputMemFileNameGen: (Long)=>String): MneDurableOutputSession[V] = {
    val ret = new MneDurableOutputSession[V] (
        serviceName, durableTypes, entityFactoryProxies,
        slotKeyId, partitionPoolSize, durableDirectory, outputMemFileNameGen)
    ret
  }
}

