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
import java.nio.file.Path
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

class MneDurableOutputSession[V: ClassTag]
    extends DurableOutputSession[V, NonVolatileMemAllocator] {

  private var _baseDir: Path = null
  private var _flist: ArrayBuffer[File] = new ArrayBuffer[File]
  private var _outputFile: File = null
  private var _outPrefix: String = null
  private var _outidx: Long = 0L

  def baseDir = _baseDir

  def baseDir_=(value: Path): Unit = _baseDir = value

  def fileList = _flist.toArray

  def outputPrefix = _outPrefix

  def outputPrefix_=(value: String): Unit = _outPrefix = value

  protected def genNextPoolFile(): File = {
    val file = new File(baseDir.toFile(), f"${_outPrefix}_${_outidx}%05d.mne")
    _outidx = _outidx + 1
    _flist += file
    file
  }

  def setOutputFile(file: File): Unit = {
    _outputFile = file
  }

  def getOutputFile: File = _outputFile

  override def initNextPool(): Boolean = {
    var ret: Boolean = false
    if (null != getAllocator) {
      getAllocator.close();
      setAllocator(null);
    }
    setOutputFile(genNextPoolFile);
    m_act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService(getServiceName),
      getPoolSize, getOutputFile.toString, true);
    if (null != getAllocator) {
      m_newpool = true;
      ret = true;
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
    baseDir: Path,
    outputPrefix: String): MneDurableOutputSession[V] = {
    var ret = new MneDurableOutputSession[V]
    ret.setServiceName(serviceName)
    ret.setDurableTypes(durableTypes)
    ret.setEntityFactoryProxies(entityFactoryProxies)
    ret.setSlotKeyId(slotKeyId)
    ret.setPoolSize(partitionPoolSize)
    ret.baseDir = baseDir
    ret.outputPrefix = outputPrefix
    ret
  }
}

