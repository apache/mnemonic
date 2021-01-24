### Generate dataset


```bash
  $ tools/gen_data.py <the number of dimensions> <the number of vectors> [-s seed]
```


### Configure Apache Spark

```text
# conf/spark-defaults.conf for local mode

spark.master local[4,0]

spark.executor.memory <size>

spark.driver.memory <size>

spark.local.dir <dir>

spark.jars <dir>/target/service-dist/mnemonic-pmalloc-service-<version>-linux-x86_64.jar,<dir>/target/service-dist/mnemonic-nvml-pmem-service-<version>-linux-x86_64.jar


```


### Run Apache Spark Benchmark Workloads


```bash
  # regular version
  $ $SPARK_HOME/bin/spark-submit --class org.apache.mnemonic.bench.RegularKMeans --conf spark.durable-basedir=./mne mnemonic/mnemonic-benches/mnemonic-spark-kmeans-bench/target/mnemonic-spark-kmeans-bench-<version>.jar <path-to-dataset-file>

  # durable version
  $ $SPARK_HOME/bin/spark-submit --class org.apache.mnemonic.bench.DurableKMeans --conf spark.durable-basedir=./mne mnemonic/mnemonic-benches/mnemonic-spark-kmeans-bench/target/mnemonic-spark-kmeans-bench-<version>.jar <path-to-dataset-file>
```
