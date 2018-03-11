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

package org.apache.mnemonic.service.computing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.mnemonic.EntityFactoryProxyHelper;
import org.apache.mnemonic.NonVolatileMemAllocator;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.Utils;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.collections.DurableSinglyLinkedList;
import org.apache.mnemonic.collections.DurableSinglyLinkedListFactory;
import org.apache.mnemonic.collections.SinglyLinkedNode;
import org.apache.mnemonic.collections.SinglyLinkedNodeFactory;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Assert;

/**
 * A Test case suit, verify and demo the durable native computing services (DNCS) infra.
 *
 */

public class DurableSinglyLinkedListNGSortTest {
  public static String uri = "./pobj_NodeValue_sorting.dat";
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

  @SuppressWarnings("unchecked")
  @Test(enabled = true)
  public void testDurableSinglyLinkedListWithPerson() throws NoSuchMethodException, ClassNotFoundException {

    int elem_count = 20;

    DurableType listgftypes[] = {DurableType.DURABLE};
    EntityFactoryProxy listefproxies[] = {new EntityFactoryProxyHelper<Person>(Person.class)};

    SinglyLinkedNode<Person<Long>> firstnv = SinglyLinkedNodeFactory.create(m_act, listefproxies,
        listgftypes, false);

    SinglyLinkedNode<Person<Long>> nextnv = firstnv;

    Person<Long> person = null;
    long val;
    SinglyLinkedNode<Person<Long>> newnv;
    for (int i = 0; i < elem_count; ++i) {
      person = (Person<Long>) listefproxies[0].create(m_act, null, null, false);
      person.setAge((short) m_rand.nextInt(50));
      person.setName(String.format("Name: [%s]", Utils.genRandomString()), true);
      nextnv.setItem(person, false);
      if (i + 1 == elem_count) {
        break;
      }
      newnv = SinglyLinkedNodeFactory.create(m_act, listefproxies, listgftypes, false);
      nextnv.setNext(newnv, false);
      nextnv = newnv;
    }

    Person<Long> eval;
    SinglyLinkedNode<Person<Long>> iternv = firstnv;
    System.out.printf(" --- Stage 1 Generated---\n");
    long agesum1 = 0L;
    while (null != iternv) {
      eval = iternv.getItem();
      Assert.assertNotNull(eval);
      eval.testOutputAge();
      agesum1 += eval.getAge();
      iternv = iternv.getNext();
    }
    System.out.printf("\n");

    long handler = firstnv.getHandler();

    DurableSinglyLinkedList<Person<Long>> list2 = DurableSinglyLinkedListFactory.restore(m_act, listefproxies,
        listgftypes, handler, false);

    System.out.printf("--- Stage 2 Restored--- \n");
    long agesum2 = 0L;
    for (Person<Long> eval2 : list2) {
      Assert.assertNotNull(eval2);
      eval2.testOutputAge();
      agesum2 += eval2.getAge();
    }
    System.out.printf("\n");
    Assert.assertEquals(agesum1, agesum2);

    GeneralComputingService gcsvr = Utils.getGeneralComputingService("sort");
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
    long[] ret = gcsvr.perform("tensor_bubble", vinfos);

    Assert.assertEquals(1, ret.length);
    long handler2 = ret[0];
    Assert.assertNotEquals(0L, handler2);

    DurableSinglyLinkedList<Person<Long>> list3 = DurableSinglyLinkedListFactory.restore(m_act, listefproxies,
        listgftypes, handler2, false);

    System.out.printf("--- Stage 3 Sorted--- \n");
    long agesum3 = 0L;
    int preage = -1;
    for (Person<Long> eval3 : list3) {
      Assert.assertNotNull(eval3);
      eval3.testOutputAge();
      agesum3 += eval3.getAge();
      if (preage >= 0) {
        Assert.assertTrue(eval3.getAge() >= preage);
      }
      preage = eval3.getAge();
    }
    System.out.printf("\n");
    Assert.assertEquals(agesum2, agesum3);
  }

  @Test(enabled = true)
  public void testDurableSinglyLinkedListValue() throws NoSuchMethodException, ClassNotFoundException {

    long[][] fieldinfo = null;
    int elem_count = 20;
    long slotKeyId = 10;

    DurableType[] elem_gftypes = {DurableType.DOUBLE};
    EntityFactoryProxy[] elem_efproxies = null;

    DurableType linkedgftypes[] = {DurableType.DURABLE, DurableType.DOUBLE};
    EntityFactoryProxy linkedefproxies[] = {
        new EntityFactoryProxyHelper<SinglyLinkedNode>(SinglyLinkedNode.class, 1)};

    DurableType listgftypes[] = {DurableType.DURABLE, DurableType.DOUBLE};
    EntityFactoryProxy listefproxies[] = {
        new EntityFactoryProxyHelper<DurableSinglyLinkedList>(DurableSinglyLinkedList.class, 1)};

    SinglyLinkedNode<SinglyLinkedNode<Double>> nextnv = null, pre_nextnv = null;
    SinglyLinkedNode<Double> elem = null, pre_elem = null, first_elem = null;

    Long linkhandler = 0L;

    System.out.printf(" --- Stage 1 Generated --- \n");

    pre_nextnv = null;
    Double val;
    double valsum1 = 0.0;
    for (int i = 0; i < elem_count; ++i) {
      first_elem = null;
      pre_elem = null;
      for (int v = 0; v < 12; ++v) {
        elem = SinglyLinkedNodeFactory.create(m_act, elem_efproxies, elem_gftypes, false);
        val = m_rand.nextDouble();
        elem.setItem(val, false);
        if (null == pre_elem) {
          first_elem = elem;
        } else {
          pre_elem.setNext(elem, false);
        }
        pre_elem = elem;
        System.out.printf("%f ", val);
        valsum1 += val;
      }

      nextnv = SinglyLinkedNodeFactory.create(m_act, linkedefproxies, linkedgftypes, false);
      nextnv.setItem(first_elem, false);
      if (null == pre_nextnv) {
        linkhandler = nextnv.getHandler();
        fieldinfo = nextnv.getNativeFieldInfo();
      } else {
        pre_nextnv.setNext(nextnv, false);
      }
      pre_nextnv = nextnv;
      System.out.printf(" generated an item... \n");
    }
    m_act.setHandler(slotKeyId, linkhandler);

    long handler = m_act.getHandler(slotKeyId);

    DurableSinglyLinkedList<DurableSinglyLinkedList<Double>> linkedvals = DurableSinglyLinkedListFactory.restore(m_act,
        listefproxies, listgftypes, handler, false);
    Iterator<DurableSinglyLinkedList<Double>> iter = linkedvals.iterator();
    Iterator<Double> elemiter = null;

    System.out.printf(" --- Stage 2 Restored --- \n");
    double val2, valsum2 = 0.0;
    while (iter.hasNext()) {
      elemiter = iter.next().iterator();
      while (elemiter.hasNext()) {
        val2 = elemiter.next();
        System.out.printf("%f ", val2);
        valsum2 += val2;
      }
      System.out.printf(" Fetched an item... \n");
    }
    Assert.assertEquals(valsum1, valsum2, 0.0001);

    GeneralComputingService gcsvr = Utils.getGeneralComputingService("sort");
    ValueInfo vinfo = new ValueInfo();
    List<long[][]> objstack = new ArrayList<long[][]>();
    objstack.add(fieldinfo);
    objstack.add(fieldinfo);
    long[][] fidinfostack = {{2L, 1L}, {2L, 1L}};
    vinfo.handler = handler;
    vinfo.transtable = m_act.getTranslateTable();
    vinfo.dtype = DurableType.DOUBLE;
    vinfo.frames = Utils.genNativeParamForm(objstack, fidinfostack);
    ValueInfo[] vinfos = {vinfo};
    gcsvr.perform("tensor_bubble", vinfos);

    // Assert.assert, expected);(plist, plist2);
    DurableSinglyLinkedList<DurableSinglyLinkedList<Double>> linkedvals3 = DurableSinglyLinkedListFactory.restore(m_act,
        listefproxies, listgftypes, handler, false);
    Iterator<DurableSinglyLinkedList<Double>> iter3 = linkedvals3.iterator();
    Iterator<Double> elemiter3 = null;

    System.out.printf(" --- Stage 3 Sorted --- \n");
    double val3, valsum3 = 0.0, preval3;
    while (iter3.hasNext()) {
      elemiter3 = iter3.next().iterator();
      preval3 = 0.0;
      while (elemiter3.hasNext()) {
        val3 = elemiter3.next();
        System.out.printf("%f ", val3);
        valsum3 += val3;
        if (preval3 != 0.0) {
          Assert.assertTrue(val3 >= preval3);
        }
        preval3 = val3;
      }
      System.out.printf(" Fetched an item... \n");
    }
    Assert.assertEquals(valsum2, valsum3, 0.0001);
  }

}
