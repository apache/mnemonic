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
import java.util.concurrent.TimeUnit

import scala.reflect.ClassTag
import scala.collection.JavaConverters._
import org.apache.mnemonic.ConfigurationException
import org.apache.mnemonic.DurableType
import org.apache.mnemonic.EntityFactoryProxy
import org.apache.mnemonic.NonVolatileMemAllocator
import org.apache.mnemonic.Utils
import org.apache.mnemonic.collections.DurableSinglyLinkedList
import org.apache.mnemonic.collections.DurableSinglyLinkedListFactory
import org.apache.mnemonic.sessions.{DurableInputSession, ObjectCreator, SessionIterator, TransformFunction}

case class SessionIteratorState(var position: Int, var streamMode: Option[Boolean])

private[spark] class MneDurableInputSession[V: ClassTag, T: ClassTag] (
    serviceName: String,
    durableTypes: Array[DurableType],
    entityFactoryProxies: Array[EntityFactoryProxy],
    slotKeyId: Long,
    var memPoolListGen: ()=>Option[Array[File]],
    outsess: MneDurableOutputSession[V], /*would be closed once no more element from source iterator*/
    srciterator: Iterator[T],
    f: (T, ObjectCreator[V, NonVolatileMemAllocator]) => Option[V],
    claimfile: File)
    extends DurableInputSession[V, NonVolatileMemAllocator, T, SessionIteratorState] {


  var iteratorHolder: SessionIterator[V, NonVolatileMemAllocator, T, SessionIteratorState] = null

  var streamMode: Option[Boolean] = None
  initialize

  def initialize: Unit = {
    setServiceName(serviceName)
    setDurableTypes(durableTypes)
    setEntityFactoryProxies(entityFactoryProxies)
    setSlotKeyId(slotKeyId)
    setOutSession(outsess)
    setTransformFunction(new TransformFunction[V, NonVolatileMemAllocator, T] {
      override def transform(value:T, objcreator:ObjectCreator[V, NonVolatileMemAllocator]):V = {
         f(value, objcreator).getOrElse(null).asInstanceOf[V]
      }
    })
  }

  def updateMode: Option[Boolean] = {
    if (null != srciterator && null != getOutSession &&
        null != getTransformFunction && null != claimfile) {
      streamMode = Option(true)
    } else if (null != memPoolListGen) {
      streamMode = Option(false)
    } else {
      throw new ConfigurationException("Cannot determine which mode Input Session works on")
    }
    streamMode
  }

  def transformAhead(): Unit = {
    if (updateMode.get) {
      streamMode = Option(tryClaim)
    }
    if (streamMode.get) {
      try {
        for (item <- srciterator) {
          f(item, outsess) match {
            case Some(res) => outsess.post(res)
            case None =>
          }
        }
      } finally {
        outsess.close
        setOutSession(null)
        setTransformFunction(null)
      }
      streamMode = Option(false)
    }
  }

  override def init(sessiter: SessionIterator[V, NonVolatileMemAllocator, T, SessionIteratorState]): Boolean = {
    sessiter.setState(SessionIteratorState(0, streamMode))
    null != sessiter.getState
  }

  def waitNextPool(file: File): Unit = {
    val lckfile: File = new File(file.toString + ".lck")
    while (lckfile.exists) {
      TimeUnit.SECONDS.sleep(1)
    }
  }

  def tryClaim: Boolean = {
    if (null == claimfile) {
      false
    } else {
      if (claimfile.exists) {
        false
      } else {
        new FileOutputStream(claimfile).close
        true
      }
    }
  }

  override def initNextPool(sessiter: SessionIterator[V, NonVolatileMemAllocator, T, SessionIteratorState]): Boolean = {
    var ret: Boolean = false
    var flist: Array[File] = null
    val state: SessionIteratorState = sessiter.getState

    if (null != sessiter.getAllocator) {
      sessiter.getAllocator.close
      sessiter.setAllocator(null)
    }

    flist = memPoolListGen().getOrElse(null)
    if (state.streamMode.isEmpty) {
      state.streamMode = Option(tryClaim)
      if (state.streamMode.get) {
        println(s"Input Session run in stream mode with ${claimfile}")
      } else {
        println(s"Input Session run in durable mode with ${claimfile}")
      }
    }

    if (!state.streamMode.get) {
      if (null != flist && flist.length > state.position) {
        waitNextPool(flist(state.position))
        sessiter.setAllocator(new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService(
          getServiceName), 1024000L, flist(state.position).toString, false))
        assert(null != sessiter.getAllocator)
        sessiter.setHandler(sessiter.getAllocator.getHandler(getSlotKeyId))
        if (0L != sessiter.getHandler) {
          val dsllist: DurableSinglyLinkedList[V] = DurableSinglyLinkedListFactory.restore(
            sessiter.getAllocator, getEntityFactoryProxies, getDurableTypes, sessiter.getHandler, false)
          if (null != dsllist) {
            sessiter.setIterator(dsllist.iterator)
            ret = null != sessiter.getIterator
            if (ret) {
              println(s"Input Session obtained the iterator of ${flist(state.position)}")
            }
          } else {
            throw new ConfigurationException("The singly linked list cannot be restored")
          }
        } else {
          throw new ConfigurationException("The value of slot handler is null")
        }
        state.position += 1
      }
    } else {
      sessiter.setSourceIterator(srciterator.asJava)
      ret = null != sessiter.getSourceIterator
      if (ret) {
        println(s"Input Session obtained the iterator of upstream with ${claimfile}")
      }
    }
    ret
  }

  override def iterator: SessionIterator[V, NonVolatileMemAllocator, T, SessionIteratorState] = {
    if (null != iteratorHolder) {
      iteratorHolder.close
      println("force to close an input session iterator prematurely")
    }
    iteratorHolder = super.iterator
    iteratorHolder
  }

  override def close: Unit = {
    super.close();
    if (null != getOutSession) {
      getOutSession.close
      setOutSession(null)
    }
    if (null != iteratorHolder) {
      iteratorHolder.close
      iteratorHolder = null
    }
  }

}

object MneDurableInputSession {
  def apply[V: ClassTag](
                          serviceName: String,
                          durableTypes: Array[DurableType],
                          entityFactoryProxies: Array[EntityFactoryProxy],
                          slotKeyId: Long,
                          memPoolListGen: ()=>Option[Array[File]]): MneDurableInputSession[V, Nothing] = {
    val ret = new MneDurableInputSession[V, Nothing] (
        serviceName, durableTypes, entityFactoryProxies,
        slotKeyId, memPoolListGen, null, null, null, null)
    ret
  }
  def apply[V: ClassTag, T: ClassTag](
                          serviceName: String,
                          durableTypes: Array[DurableType],
                          entityFactoryProxies: Array[EntityFactoryProxy],
                          slotKeyId: Long,
                          memPoolListGen: ()=>Option[Array[File]],
                          outsess: MneDurableOutputSession[V],
                          srciterator: Iterator[T],
                          f: (T, ObjectCreator[V, NonVolatileMemAllocator]) => Option[V],
                          claimfile: File):
                          MneDurableInputSession[V, T] = {
    val ret = new MneDurableInputSession[V, T] (
      serviceName, durableTypes, entityFactoryProxies,
      slotKeyId, memPoolListGen, outsess, srciterator, f, claimfile)
    ret
  }
}
