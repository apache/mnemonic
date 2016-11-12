/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mnemonic.bench;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public class RegularTestFileSort implements TextFileSort {

  private Node<Long> head;
  private long[] sortinfo = new long[3];
  
  public RegularTestFileSort() {
  }

  @Override
  public void load(BufferedReader reader) throws NumberFormatException, IOException {
    String text = null;
    Node<Long> curnode = null;
    Long val;
    while ((text = reader.readLine()) != null) {
      val = Long.parseLong(text);
      if (null == curnode) {
        curnode = new Node<Long>(val);
        this.head = curnode;
      } else {
        curnode.setNext(new Node<Long>(val));
        curnode = curnode.getNext();
      }
    }
  }

  @Override
  public void doSort() {
    Node<Long> curnode, tmpnode, prevnode;
    long cntscan = 0L, cntswap = 0L, cntnoswap = 0L;
    boolean changed;
    if (null == this.head) {
      return;
    }
    do {
      ++cntscan;
      curnode = this.head;
      prevnode = null;
      changed = false;
      while (true) {
        tmpnode = curnode.getNext();
        if (null == tmpnode) {
          break;
        }
        if (curnode.getData().compareTo(tmpnode.getData()) > 0) {
          curnode.setNext(tmpnode.getNext());
          tmpnode.setNext(curnode);
          if (null == prevnode) {
            this.head = tmpnode;
          } else {
            prevnode.setNext(tmpnode);
          }
          prevnode = tmpnode;
          changed = true;
          ++cntswap;
        } else {
          prevnode = curnode;
          curnode = tmpnode;
          ++cntnoswap;
        }
      }
    } while (changed);
    this.sortinfo[0] = cntscan;
    this.sortinfo[1] = cntswap;
    this.sortinfo[2] = cntnoswap;
  }

  @Override
  public void store(BufferedWriter writer) throws IOException {
    Node<Long> curnode = this.head;
    while (null != curnode) {
      writer.write(curnode.getData().toString());
      writer.newLine();
      curnode = curnode.getNext();
    }
  }

  @Override
  public long[] getSortInfo() {
    return this.sortinfo;
  }

  @Override
  public void clear() {
    this.head = null;
  }
}
