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

bundle_path="./target/bundle_tmp"
bundle_name="./target/bundle.jar"

excluded_modules_arr=(mnemonic-utilities-service* mnemonic-nvml-pmem-service* mnemonic-nvml-vmem-service* mnemonic-pmalloc-service*)

[[ x"${bundle_path}" = x"./target/"* ]] || ( echo "The bundle tmp path must begin with ./target/"; exit 20 )
mkdir -p ${bundle_path} || rm -f ${bundle_path}/*
bn_arr=($(find . ! -path "${bundle_path}/*" -type f -name "*.pom.asc" -exec basename {} .pom.asc \; | xargs))
for del in ${excluded_modules_arr[@]}
do
  bn_arr=(${bn_arr[@]/$del})
done
if [ ${#bn_arr[@]} -eq 0 ]; then
  echo "No found any signed submodules to be bundled !"
else
  echo "There are ${#bn_arr[@]} submodules to be bundled."
fi
rm -f ${bundle_path}/*
for i in ${bn_arr[@]}
do 
  echo "Module -> " $i
  find . ! -path "${bundle_path}/*" -type f \( -name "$i.pom" -o -name "$i.jar" -o -name "$i-javadoc.jar" -o -name "$i-sources.jar" \) -exec cp {} ${bundle_path} \;
  find . ! -path "${bundle_path}/*" -type f \( -name "$i.pom.asc" -o -name "$i.jar.asc" -o -name "$i-javadoc.jar.asc" -o -name "$i-sources.jar.asc" \) -exec cp {} ${bundle_path} \;
done
cntf=($(ls -1 ${bundle_path} | xargs)); echo "There are ${#cntf[@]} files will be packed into a bundle."
rm -f ${bundle_name}
jar cf ${bundle_name} -C ${bundle_path} .
echo "The bundle has been generated as follows."
ls -1 ${bundle_name}

popd

