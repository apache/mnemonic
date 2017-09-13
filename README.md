<img src="https://mnemonic.incubator.apache.org/img/mnemonic_logo.png" width=200 />

================================

## <a href="https://mnemonic.incubator.apache.org/" target="_blank">Mnemonic Official Website</a>

[![Build Status](https://travis-ci.org/NonVolatileComputing/incubator-mnemonic.svg?branch=master)](https://travis-ci.org/NonVolatileComputing/incubator-mnemonic)
[![CircleCI](https://circleci.com/gh/NonVolatileComputing/incubator-mnemonic.svg?style=svg)](https://circleci.com/gh/NonVolatileComputing/incubator-mnemonic)

Apache Mnemonic is a non-volatile hybrid memory storage oriented library, it proposed a non-volatile/durable Java object model and durable computing service that bring several advantages to significantly improve the performance of massive real-time data processing/analytics. developers are able to use this library to design their cache-less and SerDe-less high performance applications.

### Features:

* In-place data storage on local non-volatile memory
* Durable Object Model (DOM)
* Durable Native Computing Model (DNCM)
* Object graphs lazy loading & sharing
* Auto-reclaim memory resources and Mnemonic objects
* Hierarchical cache pool for massive data caching
* Extensible memory services for new device adoption and allocation optimization
* Durable data structure collection(WIP)
* Durable computing service
* Minimize memory footprint of on-heap
* Reduce GC Overheads as the following chart shown (collected from Apache Spark experiments)
* Drop-in Hadoop MapReduce support
