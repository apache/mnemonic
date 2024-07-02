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

import os  # Import the os module to interact with the operating system
import inspect  # Import the inspect module to get information about live objects
import sys  # Import the sys module to access system-specific parameters and functions

def findHomeDir():
    # Try to get the MNEMONIC_HOME environment variable
    homedir = os.environ.get("MNEMONIC_HOME")
    
    if not homedir:  # If MNEMONIC_HOME is not set
        # Get the filename of the current file
        filename = inspect.getframeinfo(inspect.currentframe()).filename
        # Get the absolute path of the current file
        path = os.path.dirname(os.path.abspath(filename))
        
        while True:
            # Split the path into the head (everything before the last slash) and the tail (everything after)
            pathdeco = os.path.split(path)
            
            if not pathdeco[1]:  # If the tail is empty, we've reached the root directory
                # Print an error message and exit the program
                print("Not found mnemonic home directory, please check $MNEMONIC_HOME")
                sys.exit(1)
            
            # If the tail is 'mnemonic', we've found the home directory
            if pathdeco[1] == 'mnemonic':
                break
            else:
                # Move one directory up
                path = pathdeco[0]
        
        # Set homedir to the path where 'mnemonic' directory was found
        homedir = path
    
    return homedir  # Return the home directory path

