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

import argparse
import sys
import os.path
import subprocess
import os
from shlex import quote
from benchcommon import *

parser = argparse.ArgumentParser()
parser.add_argument("input", type=str, help="specify an input file that contains a set of absolute paths of data files.")
args = parser.parse_args()

with open(args.input, "r") as sfinput:
  fns = sfinput.readlines()

os.chdir(findHomeDir())

runcmdtmp = "mvn exec:exec -Pbench -pl mnemonic-benches/mnemonic-sort-bench  -Dmode={0} -Dinput={1} -Doutput={1}_{0}.out"
cmpcmdtmp = "diff {1}_{0}.out {3}_{2}.out"
rstfntmp = "{1}_{0}.out"

for efn in fns:
  fn = efn.strip().rstrip('\n')
  if fn:
    if os.path.isfile(fn):
      print("Processing {0}, run mode A".format(fn))
      subprocess.check_call(quote(runcmdtmp.format('A', fn)), shell=True);
      print("Processing {0}, run mode B".format(fn))
      subprocess.check_call(quote(runcmdtmp.format('B', fn)), shell=True);
      print("Comparing results {0} - {1}".format(rstfntmp.format('A', fn), rstfntmp.format('B', fn)))
      subprocess.check_call(quote(cmpcmdtmp.format('A', fn, 'B', fn)), shell=True);
    else:
      print("Input data file {0} does not exist.".format(fn))

print("Finished!")
