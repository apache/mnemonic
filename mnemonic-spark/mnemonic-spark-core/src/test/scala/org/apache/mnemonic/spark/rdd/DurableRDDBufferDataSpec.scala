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

import java.util.zip.CRC32
import java.util.zip.Checksum

import scala.util._
import scala.language.existentials
import org.apache.mnemonic.spark.TestSpec
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.mnemonic.spark.rdd.DurableRDDFunctions._
import org.apache.mnemonic.DurableType
import org.apache.mnemonic.DurableBuffer
import org.apache.mnemonic.Utils
import org.apache.mnemonic.EntityFactoryProxy
import org.apache.mnemonic.sessions.ObjectCreator

class DurableRDDBufferDataSpec extends TestSpec {

  val defaultServiceName = "pmalloc"
  val defaultSlotKeyId = 2L
  val defaultPartitionSize = 1024 * 1024 * 1024L
  val defaultBaseDirectory = "."
  val defaultNumOfPartitions = 8
  val defaultNumOfRecordsPerPartition = 20

  behavior of "A DurableRDD with Buffer Type Data"

  it should "supports durable buffer as its data type" in {
    val dataOffset = 8
    val conf = new SparkConf()
        .setMaster("local[*]")
        .setAppName("Test")
    val sc = new SparkContext(conf)
    val seed: RDD[Int] = sc.parallelize(
          Seq.fill(defaultNumOfPartitions)(defaultNumOfRecordsPerPartition), defaultNumOfPartitions)
    val data = seed flatMap (recnum => Seq.fill(recnum)(Random.nextInt(1024 * 1024) + 1024 * 1024)) cache
    val durdd = data.makeDurable[DurableBuffer[_]](
        defaultServiceName,
        Array(DurableType.BUFFER), Array(),
        defaultSlotKeyId, defaultPartitionSize,
        (v: Int, oc: ObjectCreator[DurableBuffer[_], _])=>
          {
            val cs = new CRC32
            cs.reset
            val buffer = oc.newDurableObjectRecord(v)
            val bary = new Array[Byte](v - dataOffset)
            if (null != buffer) {
              buffer.clear
              Random.nextBytes(bary)
              cs.update(bary, 0, bary.length)
              buffer.get.putLong(cs.getValue)
              buffer.get.put(bary)
            }
            Option(buffer)
          })
    val durtsz = durdd map (_.getSize.toInt) sum
    val derrcount = durdd map (
        buffer => {
          var chksum: Long = -1L
          val cs = new CRC32
          cs.reset
          if (null != buffer) {
            buffer.clear
            chksum = buffer.get.getLong
            val bary = new Array[Byte](buffer.get.remaining)
            buffer.get.get(bary)
            cs.update(bary)
          }
          if (chksum != cs.getValue) 1 else 0
        }) sum
    val (rerrcnt: Long, rsz: Long) = (0L, data.sum.toLong)
    val (derrcnt: Long, dsz: Long) = (derrcount.toLong, durtsz.toLong)
    durdd.reset
    assertResult((rerrcnt, rsz)) {
      (derrcnt, dsz)
    }
  }
}
