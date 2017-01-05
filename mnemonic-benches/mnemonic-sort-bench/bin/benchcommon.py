#! /usr/bin/env python

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
      if pathdeco[1] == 'incubator-mnemonic':
        break
      else:
        path = pathdeco[0]
    homedir = path
  return homedir

