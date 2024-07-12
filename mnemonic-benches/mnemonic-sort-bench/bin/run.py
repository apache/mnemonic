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

# Import necessary modules
import argparse
import sys
import os.path
import subprocess
import os
from shlex import quote
from benchcommon import *

# Initialize argument parser to handle command-line arguments
parser = argparse.ArgumentParser()
parser.add_argument("input", type=str, help="specify an input file that contains a set of absolute paths of data files.")
args = parser.parse_args()

# Read the input file which contains a list of file paths
with open(args.input, "r") as sfinput:
    fns = sfinput.readlines()

# Change the current working directory to the home directory found by findHomeDir()
os.chdir(findHomeDir())

# Templates for the commands to run and compare outputs
runcmdtmp = "mvn exec:exec -Pbench -pl mnemonic-benches/mnemonic-sort-bench  -Dmode={0} -Dinput={1} -Doutput={1}_{0}.out"
cmpcmdtmp = "diff {1}_{0}.out {3}_{2}.out"
rstfntmp = "{1}_{0}.out"

# Loop through each file name obtained from the input file
for efn in fns:
    fn = efn.strip().rstrip('\n')  # Strip any extra whitespace and newlines
    if fn:  # Ensure the filename is not empty
        if os.path.isfile(fn):  # Check if the file exists
            # Process the file in mode A
            print("Processing {0}, run mode A".format(fn))
            subprocess.check_call(quote(runcmdtmp.format('A', fn)), shell=True)
            
            # Process the file in mode B
            print("Processing {0}, run mode B".format(fn))
            subprocess.check_call(quote(runcmdtmp.format('B', fn)), shell=True)
            
            # Compare the results of mode A and mode B
            print("Comparing results {0} - {1}".format(rstfntmp.format('A', fn), rstfntmp.format('B', fn)))
            subprocess.check_call(quote(cmpcmdtmp.format('A', fn, 'B', fn)), shell=True)
        else:
            # Print an error message if the file does not exist
            print("Input data file {0} does not exist.".format(fn))

# Indicate that the processing is finished
print("Finished!")

