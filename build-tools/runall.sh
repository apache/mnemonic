#!/bin/bash

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

if [ -z "${MNEMONIC_HOME}" ]; then
  source "$(dirname "$0")/find-mnemonic-home.sh" || { echo "Not found find-mnemonic-home.sh script."; exit 10; }
fi
pushd "$MNEMONIC_HOME" || { echo "the environment variable \$MNEMONIC_HOME contains invalid home directory of Mnemonic project."; exit 11; }

echo [INFO] Cleaning up and re-building...
git ls-files --error-unmatch pom.xml > /dev/null 2>&1 && git clean -xdf > /dev/null

if [ ! -d "testlog" ]
then
mkdir testlog
fi

mvn clean package install > testlog/build.log
if [ $? -gt 0 ]
then
echo [ERROR] Build failed, please check package dependency and refer to testlog/build.log for error messages.
exit 1
fi
echo [SUCCESS] Build Success!

echo [INFO] Running mnemonic example...
mvn exec:exec -Pexample -pl mnemonic-examples > testlog/mnemonic-example.log
if [ $? -gt 0 ]
then
echo [ERROR] This example requires \"vmem\" memory service to run, please check if \"vmem\" has been configured correctly! If \"vmem\" is installed, please refer to testlog/mnemonic-example.log for detailed information.
exit 1
fi
echo [SUCCESS] Mnemonic example is completed!

echo [INFO] Running DurablePersonNGTest test case for \"mnemonic-core\"...
mvn -Dtest=DurablePersonNGTest test -pl mnemonic-core -DskipTests=false > testlog/DurablePersonNGTest.log
if [ $? -gt 0 ]
then
echo [ERROR] This test case requires \"pmalloc\" memory service to pass, please check if \"pmalloc\" has been configured correctly! If \"pmalloc\" is installed, please refer to testlog/DurablePersonNGTest.log for detailed information.
exit 1
fi
echo [SUCCESS] Test case DurablePersonNGTest for \"mnemonic-core\" is completed!

echo [INFO] Running NonVolatileMemAllocatorNGTest test case for \"mnemonic-core\"...
mvn -Dtest=NonVolatileMemAllocatorNGTest test -pl mnemonic-core -DskipTests=false > testlog/NonVolatileMemAllocatorNGTest.log
if [ $? -gt 0 ]
then
echo [ERROR] This test case requires \"pmalloc\" memory service to pass, please check if \"pmalloc\" has been configured correctly! If \"pmalloc\" is installed, please refer to testlog/NonVolatileMemAllocatorNGTest.log for detailed information.
exit 1
fi
echo [SUCCESS] Test case NonVolatileMemAllocatorNGTest for \"mnemonic-core\" is completed!

echo [INFO] Running VolatileMemAllocatorNGTest test case of \"mnemonic-core\"...
mvn -Dtest=VolatileMemAllocatorNGTest test -pl mnemonic-core -DskipTests=false > testlog/VolatileMemAllocatorNGTest.log
if [ $? -gt 0 ]
then
echo [ERROR] This example requires \"vmem\" memory service to run, please check if \"vmem\" has been configured correctly! If \"vmem\" is installed, please refer to testlog/VolatileMemAllocatorNGTest.log for detailed information.
exit 1
fi
echo [SUCCESS] Test case VolatileMemAllocatorNGTest for \"mnemonic-core\" is completed!

echo [INFO] Running MemClusteringNGTest for \"mnemonic-core\"...
mvn -Dtest=MemClusteringNGTest test -pl mnemonic-core -DskipTests=false > testlog/MemClusteringNGTest.log
if [ $? -gt 0 ]
then
echo [ERROR] This example requires \"vmem\" memory service to run, please check if \"vmem\" has been configured correctly! If \"vmem\" is installed, please refer to testlog/MemClusteringNGTest.log for detailed information.
exit 1
fi
echo [SUCCESS] Test case MemClusteringNGTest for \"mnemonic-core\" is completed!

echo [INFO] Running DurableSinglyLinkedListNGTest for \"mnemonic-collection\"...
mvn -Dtest=DurableSinglyLinkedListNGTest  test -pl mnemonic-collections -DskipTests=false > testlog/DurableSinglyLinkedListNGTest.log
if [ $? -gt 0 ]
then
echo [ERROR] This test case requires \"pmalloc\" memory service to pass, please check if \"pmalloc\" has been configured correctly! If \"pmalloc\" is installed, please refer to testlog/DurableSinglyLinkedListNGTest.log for detailed information.
exit 1
fi
echo [SUCCESS] Test case DurableSinglyLinkedListNGTest for \"mnemonic-collection\" is completed!

echo [INFO] Running DurablePersonNGTest for \"mnemonic-collection\"...
mvn -Dtest=DurablePersonNGTest  test -pl mnemonic-collections -DskipTests=false > testlog/DurablePersonNGTest.log
if [ $? -gt 0 ]
then
echo [ERROR] This test case requires \"pmalloc\" memory service to pass, please check if \"pmalloc\" has been configured correctly! If \"pmalloc\" is installed, please refer to testlog/DurablePersonNGTest.log for detailed information.
exit 1
fi
echo [SUCCESS] Test case DurablePersonNGTest for \"mnemonic-collection\" is completed!

echo [INFO] Running DurableSinglyLinkedListNGPrintTest for \"mnemonic-computing-services/mnemonic-utilities-service\"...
mvn -Dtest=DurableSinglyLinkedListNGPrintTest test -pl mnemonic-computing-services/mnemonic-utilities-service -DskipTests=false > testlog/DurableSinglyLinkedListNGPrintTest.log
if [ $? -gt 0 ]
then
echo [ERROR] This test case requires \"pmalloc\" memory service to pass, please check if \"pmalloc\" has been configured correctly! If \"pmalloc\" is installed, please refer to testlog/DurableSinglyLinkedListNGPrintTest.log for detailed information.
exit 1
fi
echo [SUCCESS] Test case DurableSinglyLinkedListNGPrintTest for \"mnemonic-computing-services/mnemonic-utilities-service\" is completed!

echo [INFO] Running DurableSinglyLinkedListNGSortTest for \"mnemonic-computing-services/mnemonic-utilities-service\"...
mvn -Dtest=DurableSinglyLinkedListNGSortTest test -pl mnemonic-computing-services/mnemonic-utilities-service -DskipTests=false > testlog/DurableSinglyLinkedListNGSortTest.log
if [ $? -gt 0 ]
then
echo [ERROR] This test case requires \"pmalloc\" memory service to pass, please check if \"pmalloc\" has been configured correctly! If \"pmalloc\" is installed, please refer to testlog/DurableSinglyLinkedListNGSortTest.log for detailed information.
exit 1
fi
echo [SUCCESS] Test case DurableSinglyLinkedListNGSortTest for \"mnemonic-computing-services/mnemonic-utilities-service\" is completed!

echo [INFO] Running MneMapreducePersonDataTest for \"mnemonic-hadoop/mnemonic-hadoop-mapreduce\"...
mvn -Dtest=MneMapreducePersonDataTest test -pl mnemonic-hadoop/mnemonic-hadoop-mapreduce -DskipTests=false > testlog/MneMapreducePersonDataTest.log
if [ $? -gt 0 ]
then
echo [ERROR] This test case requires \"pmalloc\" memory service to pass, please check if \"pmalloc\" has been configured correctly! If \"pmalloc\" is installed, please refer to testlog/MneMapreduceIOTest.log for detailed information.
exit 1
fi
echo [SUCCESS] Test case MneMapreduceIOTest for \"mnemonic-hadoop/mnemonic-hadoop-mapreduce\" is completed!

echo [DONE] All test cases are completed! Log files are available under folder testlog!

popd
