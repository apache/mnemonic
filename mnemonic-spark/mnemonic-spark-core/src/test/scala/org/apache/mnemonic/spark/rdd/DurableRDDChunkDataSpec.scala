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
import org.apache.mnemonic.DurableChunk
import org.apache.mnemonic.Utils
import org.apache.mnemonic.EntityFactoryProxy
import org.apache.mnemonic.sessions.ObjectCreator

class DurableRDDChunkDataSpec extends TestSpec {

  val defaultServiceName = "pmalloc"
  val defaultSlotKeyId = 2L
  val defaultPartitionSize = 1024 * 1024 * 1024L
  val defaultBaseDirectory = "."
  val defaultNumOfPartitions = 8
  val defaultNumOfRecordsPerPartition = 20

  behavior of "A DurableRDD with Chunk Type Data"

  it should "supports durable chunk as its data type" in {
    val dataOffset = 8
    val conf = new SparkConf()
        .setMaster("local[*]")
        .setAppName("Test")
    val sc = new SparkContext(conf)
    val seed: RDD[Int] = sc.parallelize(
          Seq.fill(defaultNumOfPartitions)(defaultNumOfRecordsPerPartition), defaultNumOfPartitions)
    val data = seed flatMap (recnum => Seq.fill(recnum)(Random.nextInt(1024 * 1024) + 1024 * 1024)) cache
    val durdd = data.makeDurable[DurableChunk[_]](
        defaultServiceName,
        Array(DurableType.CHUNK), Array(),
        defaultSlotKeyId, defaultPartitionSize,
        (v: Int, oc: ObjectCreator[DurableChunk[_], _])=>
          {
            val cs = new CRC32
            cs.reset
            val unsafe = Utils.getUnsafe
            val chunk = oc.newDurableObjectRecord(v)
            var b: Byte = 0
            if (null != chunk) {
              for (i <- dataOffset until chunk.getSize.toInt) {
                b = Random.nextInt(255).asInstanceOf[Byte]
                unsafe.putByte(chunk.get + i, b)
                cs.update(b)
              }
              unsafe.putLong(chunk.get, cs.getValue)
            }
            Option(chunk)
          })
    val durtsz = durdd map (_.getSize.toInt) sum
    val derrcount = durdd map (
        chunk => {
          val unsafe = Utils.getUnsafe
          var chksum: Long = -1L
          val cs = new CRC32
          cs.reset
          if (null != chunk) {
            var b: Byte = 0
            for (j <- dataOffset until chunk.getSize.toInt) {
              b = unsafe.getByte(chunk.get + j)
              cs.update(b)
            }
            chksum = unsafe.getLong(chunk.get)
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
