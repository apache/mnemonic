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

import org.apache.mnemonic.ConfigurationException
import org.apache.mnemonic.DurableType
import org.apache.mnemonic.EntityFactoryProxy
import org.apache.mnemonic.NonVolatileMemAllocator
import org.apache.mnemonic.Utils
import org.apache.mnemonic.collections.DurableSinglyLinkedList
import org.apache.mnemonic.collections.DurableSinglyLinkedListFactory
import org.apache.mnemonic.sessions.DurableInputSession
import org.apache.mnemonic.sessions.SessionIterator

class MneDurableInputSession[V: ClassTag] (
    serviceName: String,
    durableTypes: Array[DurableType],
    entityFactoryProxies: Array[EntityFactoryProxy],
    slotKeyId: Long,
    memPoolList: Array[File] )
    extends DurableInputSession[V, NonVolatileMemAllocator] {

  var memPools: Array[File] = null

  private var flistIter:Iterator[File] = null

  initialize(serviceName, durableTypes, entityFactoryProxies,
      slotKeyId, memPoolList)

  def initialize (
    serviceName: String,
    durableTypes: Array[DurableType],
    entityFactoryProxies: Array[EntityFactoryProxy],
    slotKeyId: Long,
    memPoolList: Array[File]) {
    setServiceName(serviceName)
    setDurableTypes(durableTypes)
    setEntityFactoryProxies(entityFactoryProxies)
    setSlotKeyId(slotKeyId);
    memPools = memPoolList
    if (null != memPools) {
      flistIter = memPools.iterator
    }
  }

  override def initNextPool(sessiter: SessionIterator[V, NonVolatileMemAllocator]): Boolean = {
    var ret: Boolean = false
    if (null != sessiter.getAllocator) {
      sessiter.getAllocator.close
      sessiter.setAllocator(null)
    }
    if (null != flistIter && flistIter.hasNext) {
      sessiter.setAllocator(new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService(
        getServiceName), 1024000L, flistIter.next.toString, true));
      if (null != sessiter.getAllocator) {
        sessiter.setHandler(sessiter.getAllocator.getHandler(getSlotKeyId))
        if (0L != sessiter.getHandler) {
          val dsllist: DurableSinglyLinkedList[V] = DurableSinglyLinkedListFactory.restore(
            sessiter.getAllocator, getEntityFactoryProxies, getDurableTypes, sessiter.getHandler, false)
          if (null != dsllist) {
            sessiter.setIterator(dsllist.iterator)
            ret = null != sessiter.getIterator
          }
        }
      }
    }
    ret
  }

}

object MneDurableInputSession {
  def apply[V: ClassTag](
    serviceName: String,
    durableTypes: Array[DurableType],
    entityFactoryProxies: Array[EntityFactoryProxy],
    slotKeyId: Long,
    memPoolList: Array[File]): MneDurableInputSession[V] = {
    val ret = new MneDurableInputSession[V] (
        serviceName, durableTypes, entityFactoryProxies,
        slotKeyId, memPoolList)
    ret
  }
}
