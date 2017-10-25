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

/*
 * A variant of Apache Spark KMeansSample.scala @52facb0062a4253fa45ac0c633d0510a9b684a62
 */

// scalastyle:off println
package org.apache.mnemonic.bench

import scala.util._
import scala.language.existentials
import scala.io.Source
import java.nio.DoubleBuffer

import org.apache.spark.{SparkConf, SparkContext}
// $example on$
import org.apache.spark.mllib.clustering.{KMeans, KMeansModel}
import org.apache.spark.mllib.linalg.Vectors

import org.apache.mnemonic.spark.rdd.DurableRDDFunctions._
import org.apache.mnemonic.DurableType
import org.apache.mnemonic.DurableBuffer
import org.apache.mnemonic.Utils
import org.apache.mnemonic.EntityFactoryProxy
import org.apache.mnemonic.sessions.ObjectCreator

// $example off$

object DurableKMeans {

  val defaultServiceName = "pmalloc"
  val defaultSlotKeyId = 2L
  val defaultPartitionSize = 1024 * 1024 * 1024L
  val defaultBaseDirectory = "."

  def firstLine(fn: String): Option[String] = {
    val src = Source.fromFile(fn)
    try {
        src.getLines.find(_ => true)
    } finally {
      src.close()
    }
  }

  def main(args: Array[String]) {

    if (args.length == 0) {
        println("no input file name")
        System.exit(1)
    }

    val conf = new SparkConf().setAppName("DurableKMeans")
    val sc = new SparkContext(conf)

    var vectorLen:Int = 0

    firstLine(args(0)) match {
        case Some(fline) => { vectorLen = fline.split(' ').length }
        case None => { println("Input file is Empty"); System.exit(2) }
    }
    
    val vectorLenInBytes = vectorLen * (java.lang.Double.SIZE / java.lang.Byte.SIZE)
    val vector = new Array[Double](vectorLen)

    val start = System.currentTimeMillis

    // $example on$
    // Load and parse the data
    val data = sc.textFile(args(0))
    val durdd = data.makeDurable[DurableBuffer[_]](
        defaultServiceName,
        Array(DurableType.BUFFER), Array(),
        defaultSlotKeyId, defaultPartitionSize,
        (v: String, oc: ObjectCreator[DurableBuffer[_], _])=>
          {
            val buffer = oc.newDurableObjectRecord(vectorLenInBytes)
            if (null != buffer) {
              buffer.clear
              buffer.get.asDoubleBuffer().put(v.split(' ').map(_.toDouble).toArray)
            }
            Option(buffer)
          })

    val parsedData = durdd.map(
        buffer => 
          {
            buffer.clear
            val dbuf: DoubleBuffer = buffer.get.asDoubleBuffer
            // println  // test code
            // for (e <- 0 to 7) print(dbuf.get(e) + " ") // test code
            // println  // test code
            dbuf.get(vector)
            Vectors.dense(vector)
          })

    // val parsedData = data.map(s => Vectors.dense(s.split(' ').map(_.toDouble))).cache()

    // Cluster the data into two classes using KMeans
    val numClusters = 2
    val numIterations = 20
    val clusters = KMeans.train(parsedData, numClusters, numIterations)

    // Evaluate clustering by computing Within Set Sum of Squared Errors
    val WSSSE = clusters.computeCost(parsedData)

    val totalTime = System.currentTimeMillis - start

    println("Within Set Sum of Squared Errors = " + WSSSE)

    println("Dimension of processed points = " + vectorLen) // verify code

    println("Total count of processed points = " + parsedData.count) // verify code

    println("Elapsed time: %1d s".format(totalTime/1000))

    // Save and load model
    //clusters.save(sc, "target/org/apache/spark/KMeansExample/KMeansModel")
    //val sameModel = KMeansModel.load(sc, "target/org/apache/spark/KMeansExample/KMeansModel")
    // $example off$

    sc.stop()
  }
}
// scalastyle:on println
