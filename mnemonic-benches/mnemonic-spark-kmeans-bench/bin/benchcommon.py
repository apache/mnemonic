#! /usr/bin/env python3

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

import os
import inspect
import sys

def findHomeDir():
  homedir = os.environ.get("MNEMONIC_HOME")
  if not homedir:
    filename = inspect.getframeinfo(inspect.currentframe()).filename
    path = os.path.dirname(os.path.abspath(filename))
    while 1:
      pathdeco = os.path.split(path)
      if not pathdeco[1]:
        print "Not found mnemonic home directory, please check \$MNEMONIC_HOME"
        sys.exit(1)
      if pathdeco[1] == 'mnemonic':
        break
      else:
        path = pathdeco[0]
    homedir = path
  return homedir

