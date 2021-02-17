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

import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.Utils;
import org.apache.mnemonic.VolatileMemAllocator;
import org.apache.mnemonic.collections.DurableSinglyLinkedList;
import org.apache.mnemonic.collections.DurableSinglyLinkedListFactory;
import org.apache.mnemonic.collections.SinglyLinkedNode;
import org.apache.mnemonic.collections.SinglyLinkedNodeFactory;
import org.apache.mnemonic.service.computing.GeneralComputingService;
import org.apache.mnemonic.service.computing.ValueInfo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DNCSTextFileSort implements TextFileSort {
  private String uri = "";
  private VolatileMemAllocator m_act = null;
  private DurableType[] elem_gftypes = {DurableType.LONG};
  private EntityFactoryProxy[] elem_efproxies = null;
  private Long head;
  private long[] sortinfo = new long[3];
  private long[][] ostack;

  public DNCSTextFileSort() {
  }

  @Override
  public void load(BufferedReader reader) throws NumberFormatException, IOException {
    if (null != m_act) {
      clear();
    }
    m_act = new VolatileMemAllocator(Utils.getVolatileMemoryAllocatorService("sysvmem"), 1024 * 1024 * 1024 * 5,
        uri);
    String text = null;
    SinglyLinkedNode<Long> curnode = null, prvnode = null, node = null;
    Long val;
    while ((text = reader.readLine()) != null) {
      val = Long.parseLong(text);
      curnode = SinglyLinkedNodeFactory.create(m_act, elem_efproxies, elem_gftypes, false);
      curnode.setItem(val, false);
      if (null == prvnode) {
        this.head = curnode.getHandler();
        this.ostack = curnode.getNativeFieldInfo();
      } else {
        prvnode.setNext(curnode, false);
      }
      prvnode = curnode;
    }
  }

  @Override
  public void doSort() {
    GeneralComputingService gcsvr = Utils.getGeneralComputingService("sort");
    ValueInfo vinfo = new ValueInfo();
    List<long[][]> objstack = new ArrayList<long[][]>();
    objstack.add(this.ostack);
    long[][] fidinfostack = {{2L, 1L}};
    vinfo.handler = this.head;
    vinfo.transtable = m_act.getTranslateTable();
    vinfo.dtype = DurableType.LONG;
    vinfo.frames = Utils.genNativeParamForm(objstack, fidinfostack);
    ValueInfo[] vinfos = {vinfo};
    long[] ret = gcsvr.perform("1dlong_bubble", vinfos);
    this.head = ret[0];
    this.sortinfo[0] = ret[1];
    this.sortinfo[1] = ret[2];
    this.sortinfo[2] = ret[3];
  }

  @Override
  public void store(BufferedWriter writer) throws IOException {
    DurableSinglyLinkedList<Long> linkvals = DurableSinglyLinkedListFactory.restore(m_act,
        elem_efproxies, elem_gftypes, this.head, false);
    Iterator<Long> elemiter = linkvals.iterator();
    Long val;
    while (elemiter.hasNext()) {
      val = elemiter.next();
      writer.write(val.toString());
      writer.newLine();
    }
  }

  @Override
  public long[] getSortInfo() {
    return this.sortinfo;
  }

  @Override
  public void clear() {
    m_act.close();
  }
}
