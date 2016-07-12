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

package org.apache.mnemonic.service.computingservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.mnemonic.NonVolatileMemAllocator;
import org.apache.mnemonic.RestorableAllocator;
import org.apache.mnemonic.Durable;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.Utils;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.collections.DurableNodeValue;
import org.apache.mnemonic.collections.DurableNodeValueFactory;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * A Test case suit, verify and demo the durable native computing services (DNCS) infra.
 *
 */

public class DurableNodeValueNGPrintTest {
  public static String uri = "./pobj_NodeValue_print.dat";
  private long cKEYCAPACITY;
  private Random m_rand;
  private NonVolatileMemAllocator m_act;

  @BeforeClass
  public void setUp() throws IOException {
    m_rand = Utils.createRandom();
    Files.deleteIfExists(Paths.get(uri));
    m_act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService("pmalloc"), 1024 * 1024 * 1024,
        uri, true);
    cKEYCAPACITY = m_act.handlerCapacity();
    for (long i = 0; i < cKEYCAPACITY; ++i) {
      m_act.setHandler(i, 0L);
    }
  }

  @AfterClass
  public void tearDown() {
    m_act.close();
  }

  @Test(enabled = true)
  public void testLinkedNodeValueWithPerson() {

    int elem_count = 10;

    DurableType listgftypes[] = {DurableType.DURABLE};
    EntityFactoryProxy listefproxies[] = {new EntityFactoryProxy() {
      @Override
      public <A extends RestorableAllocator<A>> Durable restore(A allocator, EntityFactoryProxy[] factoryproxys,
          DurableType[] gfields, long phandler, boolean autoreclaim) {
        return PersonFactory.restore(allocator, factoryproxys, gfields, phandler, autoreclaim);
      }
    } };

    DurableNodeValue<Person<Long>> firstnv = DurableNodeValueFactory.create(m_act, listefproxies, listgftypes,
        false);

    DurableNodeValue<Person<Long>> nextnv = firstnv;

    Person<Long> person = null;
    long val;
    DurableNodeValue<Person<Long>> newnv;
    for (int i = 0; i < elem_count; ++i) {
      person = PersonFactory.create(m_act);
      person.setAge((short) m_rand.nextInt(50));
      person.setName(String.format("Name: [%s]", Utils.genRandomString()), true);
      nextnv.setItem(person, false);
      if (i + 1 == elem_count) {
        break;
      }
      newnv = DurableNodeValueFactory.create(m_act, listefproxies, listgftypes, false);
      nextnv.setNext(newnv, false);
      nextnv = newnv;
    }

    Person<Long> eval;
    DurableNodeValue<Person<Long>> iternv = firstnv;
    System.out.printf(" -- Stage 1 Generated---\n");
    while (null != iternv) {
      eval = iternv.getItem();
      if (null != eval) {
        eval.testOutputAge();
      }
      iternv = iternv.getNext();
    }
    System.out.printf("\n");

    long handler = firstnv.getHandler();

    DurableNodeValue<Person<Long>> firstnv2 = DurableNodeValueFactory.restore(m_act, listefproxies, listgftypes,
        handler, false);

    System.out.printf("--- Stage 2 Restored--- \n");
    for (Person<Long> eval2 : firstnv2) {
      if (null != eval2) {
        eval2.testOutputAge();
      }
    }
    System.out.printf("\n");

    GeneralComputingService gcsvr = Utils.getGeneralComputingService("print");
    ValueInfo vinfo = new ValueInfo();
    List<long[][]> objstack = new ArrayList<long[][]>();
    objstack.add(firstnv.getNativeFieldInfo());
    objstack.add(person.getNativeFieldInfo());
    long[][] fidinfostack = {{2L, 1L}, {0L, 1L}};
    vinfo.handler = handler;
    vinfo.transtable = m_act.getTranslateTable();
    vinfo.dtype = DurableType.SHORT;
    vinfo.frames = Utils.genNativeParamForm(objstack, fidinfostack);
    ValueInfo[] vinfos = {vinfo};
    gcsvr.perform(vinfos);

  }

  @Test(enabled = true)
  public void testLinkedNodeValueWithLinkedNodeValue() {

    int elem_count = 10;
    long slotKeyId = 10;

    DurableType[] elem_gftypes = {DurableType.DOUBLE};
    EntityFactoryProxy[] elem_efproxies = null;

    DurableType linkedgftypes[] = {DurableType.DURABLE, DurableType.DOUBLE};
    EntityFactoryProxy linkedefproxies[] = {new EntityFactoryProxy() {
      @Override
      public <A extends RestorableAllocator<A>> Durable restore(A allocator, EntityFactoryProxy[] factoryproxys,
          DurableType[] gfields, long phandler, boolean autoreclaim) {
        EntityFactoryProxy[] val_efproxies = null;
        DurableType[] val_gftypes = null;
        if (null != factoryproxys && factoryproxys.length >= 2) {
          val_efproxies = Arrays.copyOfRange(factoryproxys, 1, factoryproxys.length);
        }
        if (null != gfields && gfields.length >= 2) {
          val_gftypes = Arrays.copyOfRange(gfields, 1, gfields.length);
        }
        return DurableNodeValueFactory.restore(allocator, val_efproxies, val_gftypes, phandler, autoreclaim);
      }
    } };

    DurableNodeValue<DurableNodeValue<Double>> nextnv = null, pre_nextnv = null;
    DurableNodeValue<Double> elem = null, pre_elem = null, first_elem = null;

    Long linkhandler = 0L;

    System.out.printf(" Stage 1 -testLinkedNodeValueWithLinkedNodeValue--> \n");

    pre_nextnv = null;
    Double val;
    for (int i = 0; i < elem_count; ++i) {
      first_elem = null;
      pre_elem = null;
      for (int v = 0; v < 3; ++v) {
        elem = DurableNodeValueFactory.create(m_act, elem_efproxies, elem_gftypes, false);
        val = m_rand.nextDouble();
        elem.setItem(val, false);
        if (null == pre_elem) {
          first_elem = elem;
        } else {
          pre_elem.setNext(elem, false);
        }
        pre_elem = elem;
        System.out.printf("%f ", val);
      }

      nextnv = DurableNodeValueFactory.create(m_act, linkedefproxies, linkedgftypes, false);
      nextnv.setItem(first_elem, false);
      if (null == pre_nextnv) {
        linkhandler = nextnv.getHandler();
      } else {
        pre_nextnv.setNext(nextnv, false);
      }
      pre_nextnv = nextnv;
      System.out.printf(" generated an item... \n");
    }
    m_act.setHandler(slotKeyId, linkhandler);

    long handler = m_act.getHandler(slotKeyId);

    DurableNodeValue<DurableNodeValue<Double>> linkedvals = DurableNodeValueFactory.restore(m_act,
        linkedefproxies, linkedgftypes, handler, false);
    Iterator<DurableNodeValue<Double>> iter = linkedvals.iterator();
    Iterator<Double> elemiter = null;

    System.out.printf(" Stage 2 -testLinkedNodeValueWithLinkedNodeValue--> \n");
    while (iter.hasNext()) {
      elemiter = iter.next().iterator();
      while (elemiter.hasNext()) {
        System.out.printf("%f ", elemiter.next());
      }
      System.out.printf(" Fetched an item... \n");
    }

    GeneralComputingService gcsvr = Utils.getGeneralComputingService("print");
    ValueInfo vinfo = new ValueInfo();
    List<long[][]> objstack = new ArrayList<long[][]>();
    objstack.add(linkedvals.getNativeFieldInfo());
    objstack.add(linkedvals.getNativeFieldInfo());
    long[][] fidinfostack = {{2L, 1L}, {2L, 1L}};
    vinfo.handler = handler;
    vinfo.transtable = m_act.getTranslateTable();
    vinfo.dtype = DurableType.DOUBLE;
    vinfo.frames = Utils.genNativeParamForm(objstack, fidinfostack);
    ValueInfo[] vinfos = {vinfo};
    gcsvr.perform(vinfos);

    // Assert.assert, expected);(plist, plist2);

  }

}
