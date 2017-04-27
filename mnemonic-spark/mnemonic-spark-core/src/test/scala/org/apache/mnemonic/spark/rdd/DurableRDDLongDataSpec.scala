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

class DurableRDDLongDataSpec extends TestSpec {

  val defaultServiceName = "pmalloc"
  val defaultSlotKeyId = 2L
  val defaultPartitionSize = 1024 * 1024 * 1024L
  val defaultBaseDirectory = "."
  val defaultNumOfPartitions = 8
  val defaultNumOfRecordsPerPartition = 5000

  behavior of "A DurableRDD with Long Type Data"

  it should "have unmodified values if only converting the data type from Int to Long" in {
    val conf = new SparkConf()
        .setMaster("local[*]")
        .setAppName("Test")
    val sc = new SparkContext(conf)
    // sc.getConf.getAll.foreach(println)
    // DurableRDD.setDurableBaseDir(sc, defaultBaseDirectory)
    val seed: RDD[Int] = sc.parallelize(
        Seq.fill(defaultNumOfPartitions)(defaultNumOfRecordsPerPartition), defaultNumOfPartitions)
    val data = seed flatMap (recnum => Seq.fill(recnum)(Random.nextInt)) cache //must be cached to fix rand numbers
    val durdd = data.makeDurable[Long](
        defaultServiceName,
        Array(DurableType.LONG), Array(),
        defaultSlotKeyId, defaultPartitionSize,
        (v: Int, oc: ObjectCreator[Long, _])=>
          { Some(v.asInstanceOf[Long]) })
    // data.collect().foreach(println)
    // durdd.collect().foreach(println)
    val (rcnt, rsum) = (data.count, data.sum)
    val (dcnt, dsum) = (durdd.count, durdd.sum)
    durdd.reset
    /*sys.addShutdownHook({
      DurableRDD.cleanupForApp(sc)
    })*/
    assertResult((rcnt, rsum)) {
      (dcnt, dsum)
    }
  }

}

