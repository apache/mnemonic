#! /usr/bin/env python

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

import re
import subprocess
import sys
import os

testLogDir = "testlog/"

if not os.path.exists(testLogDir):
    os.makedirs(testLogDir)

cleanupCmd = r'find mnemonic-* -type f \( -name "*.mne" -o -name "*.dat" \) -exec rm {} \;'
testCmdFile = 'tools/test.conf'
tcCmdReg = re.compile('^mvn\s.*$')
tcNameReg = re.compile('-D(?:test|suites)=(.+?)\s')
tcModuleReg = re.compile('-pl\s(.+?)\s')
with open(testCmdFile) as fp:
    for line in fp:
        match = tcCmdReg.findall(line)
        if match:
            tc_module_target_path = tcModuleReg.findall(line)[0] + "/target"
            if any(fname.endswith('.jar') for fname in os.listdir(tc_module_target_path)):
                logFilePath = testLogDir + tcNameReg.findall(line)[0] + ".log"
                print("[INFO] Running " + tcNameReg.findall(line)[0] + " test case for \"" + tcModuleReg.findall(line)[0] + "\"...")
                try:
                    #maven build
                    subprocess.check_call(match[0] + ">" + logFilePath, stderr=subprocess.STDOUT, shell=True)
                    subprocess.call(cleanupCmd, stderr=subprocess.STDOUT, shell=True)
                    print("[SUCCESS] Test case " + tcNameReg.findall(line)[0] + " for \"" + tcModuleReg.findall(line)[0]+ "\" is completed!")
                except subprocess.CalledProcessError as e:
                    print("[ERROR] Please refer to testlog/" + tcNameReg.findall(line)[0] + ".log for detailed information.")
                    sys.exit(1)
            else:
                print("[WARN] JAR file not found in " + tcNameReg.findall(line)[0] + " test case for \"" + tcModuleReg.findall(line)[0] + "\".")
print("[DONE] All test cases are completed! Log files are available under folder testlog.")
