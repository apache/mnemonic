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
 * A copy of Apache Spark KMeansSample.scala @52facb0062a4253fa45ac0c633d0510a9b684a62
 */

// scalastyle:off println
package org.apache.mnemonic.bench

import org.apache.spark.{SparkConf, SparkContext}
// $example on$
import org.apache.spark.mllib.clustering.{KMeans, KMeansModel}
import org.apache.spark.mllib.linalg.Vectors
// $example off$

object RegularKMeans {

  def main(args: Array[String]) {

    if (args.length == 0) {
        println("no input file name")
        System.exit(1)
    }

    val conf = new SparkConf().setAppName("RegularKMeans")
    val sc = new SparkContext(conf)

    val start = System.currentTimeMillis

    // $example on$
    // Load and parse the data
    val data = sc.textFile(args(0))
    //val parsedData = data.map(s => Vectors.dense(s.split(' ').map(_.toDouble))).cache()
    val parsedData = data.map(s => Vectors.dense(s.split(' ').map(_.toDouble)))

    // Cluster the data into two classes using KMeans
    val numClusters = 2
    val numIterations = 20
    val clusters = KMeans.train(parsedData, numClusters, numIterations)

    // Evaluate clustering by computing Within Set Sum of Squared Errors
    val WSSSE = clusters.computeCost(parsedData)

    val totalTime = System.currentTimeMillis - start

    println("Within Set Sum of Squared Errors = " + WSSSE)

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
