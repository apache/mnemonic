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

python build-tools/runTestCases.py

popd
