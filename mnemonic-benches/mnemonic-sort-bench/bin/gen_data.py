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

import argparse
import sys
import random
import os
from benchcommon import *

parser = argparse.ArgumentParser()
parser.add_argument("count", type=long, help="specify how many random numbers will be generated")
parser.add_argument("-s", "--seed", help="specify the random seed")
args = parser.parse_args()

if args.seed:
  seed = args.seed
else:
  seed = random.randint(0, sys.maxint)
random.seed(seed)

fn = "sort_data_{0}_{1}.dat".format(args.count, seed)
afile = open(fn, "w", 512*1024*1024 )

print("The generated data will be written to the file {0} with the seed {1}".format(fn, seed))
try:
  for i in xrange(args.count):
    line = str(random.randint(1, 999999999)) + "\n"
    afile.write(line)
    if 0 == i % 1000000:
      sys.stdout.write('.')
      sys.stdout.flush()
except ValueError:
    pass

afile.close()

print("")
print("The generated data have been written to the file {0} with the seed {1}\n".format(fn, seed))

