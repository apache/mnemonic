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

import java.io.{File, FileOutputStream}

import scala.reflect.{ClassTag, classTag}
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
  var lockFile: File = null

  initialize

  def initialize: Unit = {
    setServiceName(serviceName)
    setDurableTypes(durableTypes)
    setEntityFactoryProxies(entityFactoryProxies)
    setSlotKeyId(slotKeyId)
    setPoolSize(partitionPoolSize)
    m_recparmpair = Utils.shiftDurableParams(getDurableTypes, getEntityFactoryProxies, 1)
  }

  protected def genNextPoolFile(): File = {
    val file = new File(durableDirectory, outputMemFileNameGen(_outidx))
    _outidx += 1
    memPools += file
    file
  }

  def tryLockPart(outputFile: File): File = {
    if (outputFile.exists) {
      throw new DurableException(s"Durable memory file already exists ${outputFile}")
    }
    val lckf = new File(outputFile.toString + ".lck")
    if (lckf.exists) {
      throw new DurableException(s"${outputFile.toString} has been exclusively locked by another process")
    }
    new FileOutputStream(lckf).close
    lckf
  }

  override def initNextPool(): Boolean = {
    var ret: Boolean = false
    val outputFile = genNextPoolFile
    val lckf = tryLockPart(outputFile)
    clear
    lockFile = lckf
    val act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService(getServiceName),
      getPoolSize, outputFile.toString, true)
    if (null != act) {
      setAllocator(act)
      m_newpool = true
      ret = true
    } else {
      throw new DurableException(s"${outputFile.toString} cannot be opened by allocator")
    }
    ret
  }

  def clear(): Unit = {
    if (null != getAllocator) {
      getAllocator.close
      setAllocator(null)
    }
    if (null != lockFile) {
      lockFile.delete
      lockFile = null
    }
  }

  override def close: Unit = {
    clear
    super.close
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

