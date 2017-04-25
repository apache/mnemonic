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

import org.apache.mnemonic.spark.TestSpec
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.mnemonic.spark.rdd.DurableRDDFunctions._
import org.apache.mnemonic.DurableType
import org.apache.mnemonic.NonVolatileMemAllocator
import org.apache.mnemonic.EntityFactoryProxy
import org.apache.mnemonic.sessions.ObjectCreator

class DurableRDDSpec extends TestSpec {

  val defaultServiceName = "pmalloc"
  val defaultSlotKeyId = 2L
  val defaultPartitionSize = 1024 * 1024 * 1024L
  val defaultBaseDirectory = "."
  val defaultNumOfRecordsPerPartition = 5000L

  behavior of "A DurableRDD"

  it should "have the same sum value" in {
    val conf = new SparkConf()
        .setMaster("local[*]")
        .setAppName("Test")
    val sc = new SparkContext(conf)
    // sc.getConf.getAll.foreach(println)
    val ds = Seq(5, 6, 3, 8, 4)
    val data: RDD[Int] = sc.parallelize(ds)
    val durdd = data.makeDurable[Long](
        defaultServiceName,
        Array(DurableType.LONG), Array(),
        defaultSlotKeyId, defaultPartitionSize,
        defaultBaseDirectory,
        (v: Int, oc: ObjectCreator[Long, NonVolatileMemAllocator])=>
          { Some(v.asInstanceOf[Long]) })
    // data.collect().foreach(println)
    // durdd.collect().foreach(println)
    val (rcnt, rsum) = (data.count, data.sum)
    val (dcnt, dsum) = (durdd.count, durdd.sum)
    durdd.close
    assertResult((rcnt, rsum)) {
      (dcnt, dsum)
    }
  }

  it should "produce NoSuchElementException when head is invoked" in {
    assertThrows[NoSuchElementException] {
      Set.empty.head
    }
  }
}
