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

package org.apache.mnemonic.collections;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.mnemonic.NonVolatileMemAllocator;
import org.apache.mnemonic.RestorableAllocator;
import org.apache.mnemonic.Durable;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.Reclaim;
import org.apache.mnemonic.Utils;
import org.apache.mnemonic.DurableType;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 *
 */

public class DurableSinglyLinkedListNGTest {
  private long cKEYCAPACITY;
  private Random m_rand;
  private NonVolatileMemAllocator m_act;

  @BeforeClass
  public void setUp() {
    m_rand = Utils.createRandom();
    m_act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService("pmalloc"), 1024 * 1024 * 1024,
        "./pobj_NodeValue.dat", true);
    cKEYCAPACITY = m_act.handlerCapacity();
    m_act.setBufferReclaimer(new Reclaim<ByteBuffer>() {
      @Override
      public boolean reclaim(ByteBuffer mres, Long sz) {
        System.out.println(String.format("Reclaim Memory Buffer: %X  Size: %s", System.identityHashCode(mres),
            null == sz ? "NULL" : sz.toString()));
        return false;
      }
    });
    m_act.setChunkReclaimer(new Reclaim<Long>() {
      @Override
      public boolean reclaim(Long mres, Long sz) {
        System.out.println(String.format("Reclaim Memory Chunk: %X  Size: %s", System.identityHashCode(mres),
            null == sz ? "NULL" : sz.toString()));
        return false;
      }
    });

    for (long i = 0; i < cKEYCAPACITY; ++i) {
      m_act.setHandler(i, 0L);
    }
  }

  @AfterClass
  public void tearDown() {
    m_act.close();
  }

  @Test(enabled = false)
  public void testSingleNodeValueWithInteger() {
    int val = m_rand.nextInt();
    DurableType gtypes[] = {DurableType.INTEGER};
    DurableSinglyLinkedList<Integer> plln = DurableSinglyLinkedListFactory.create(m_act, null, gtypes, false);
    plln.setItem(val, false);
    Long handler = plln.getHandler();
    System.err.println("-------------Start to Restore Integer -----------");
    DurableSinglyLinkedList<Integer> plln2 = DurableSinglyLinkedListFactory.restore(m_act, null, gtypes, handler, 
        false);
    AssertJUnit.assertEquals(val, (int) plln2.getItem());
  }

  @Test(enabled = false)
  public void testNodeValueWithString() {
    String val = Utils.genRandomString();
    DurableType gtypes[] = {DurableType.STRING};
    DurableSinglyLinkedList<String> plln = DurableSinglyLinkedListFactory.create(m_act, null, gtypes, false);
    plln.setItem(val, false);
    Long handler = plln.getHandler();
    System.err.println("-------------Start to Restore String-----------");
    DurableSinglyLinkedList<String> plln2 = DurableSinglyLinkedListFactory.restore(m_act, null, gtypes, handler, 
        false);
    AssertJUnit.assertEquals(val, plln2.getItem());
  }

  @Test(enabled = false)
  public void testNodeValueWithPerson() {

    Person<Long> person = PersonFactory.create(m_act);
    person.setAge((short) 31);

    DurableType gtypes[] = {DurableType.DURABLE};
    EntityFactoryProxy efproxies[] = {new EntityFactoryProxy() {
      @Override
      public <A extends RestorableAllocator<A>> Durable restore(A allocator, EntityFactoryProxy[] factoryproxys,
          DurableType[] gfields, long phandler, boolean autoreclaim) {
        return PersonFactory.restore(allocator, factoryproxys, gfields, phandler, autoreclaim);
      }
    } };

    DurableSinglyLinkedList<Person<Long>> plln = DurableSinglyLinkedListFactory.create(m_act, efproxies, gtypes, false);
    plln.setItem(person, false);
    Long handler = plln.getHandler();

    DurableSinglyLinkedList<Person<Long>> plln2 = DurableSinglyLinkedListFactory.restore(m_act, efproxies, gtypes, 
        handler, false);
    AssertJUnit.assertEquals(31, (int) plln2.getItem().getAge());

  }

  @Test(enabled = false)
  public void testLinkedNodeValueWithPerson() {

    int elem_count = 10;
    List<Long> referlist = new ArrayList();

    DurableType listgftypes[] = {DurableType.DURABLE};
    EntityFactoryProxy listefproxies[] = {new EntityFactoryProxy() {
      @Override
      public <A extends RestorableAllocator<A>> Durable restore(A allocator, EntityFactoryProxy[] factoryproxys,
          DurableType[] gfields, long phandler, boolean autoreclaim) {
        return PersonFactory.restore(allocator, factoryproxys, gfields, phandler, autoreclaim);
      }
    } };

    DurableSinglyLinkedList<Person<Long>> firstnv = DurableSinglyLinkedListFactory.create(m_act, listefproxies, 
        listgftypes, false);

    DurableSinglyLinkedList<Person<Long>> nextnv = firstnv;

    Person<Long> person;
    long val;
    DurableSinglyLinkedList<Person<Long>> newnv;
    for (int i = 0; i < elem_count; ++i) {
      person = PersonFactory.create(m_act);
      person.setAge((short) m_rand.nextInt(50));
      person.setName(String.format("Name: [%s]", Utils.genRandomString()), true);
      nextnv.setItem(person, false);
      newnv = DurableSinglyLinkedListFactory.create(m_act, listefproxies, listgftypes, false);
      nextnv.setNext(newnv, false);
      nextnv = newnv;
    }

    Person<Long> eval;
    DurableSinglyLinkedList<Person<Long>> iternv = firstnv;
    while (null != iternv) {
      System.out.printf(" Stage 1 --->\n");
      eval = iternv.getItem();
      if (null != eval) {
        eval.testOutput();
      }
      iternv = iternv.getNext();
    }

    long handler = firstnv.getHandler();

    DurableSinglyLinkedList<Person<Long>> firstnv2 = DurableSinglyLinkedListFactory.restore(m_act, listefproxies, 
        listgftypes, handler, false);

    for (Person<Long> eval2 : firstnv2) {
      System.out.printf(" Stage 2 ---> \n");
      if (null != eval2) {
        eval2.testOutput();
      }
    }

    // Assert.assert, expected);(plist, plist2);

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
        return DurableSinglyLinkedListFactory.restore(allocator, val_efproxies, val_gftypes, phandler, autoreclaim);
      }
    } };

    DurableSinglyLinkedList<DurableSinglyLinkedList<Double>> nextnv = null, pre_nextnv = null;
    DurableSinglyLinkedList<Double> elem = null, pre_elem = null, first_elem = null;

    Long linkhandler = 0L;

    System.out.printf(" Stage 1 -testLinkedNodeValueWithLinkedNodeValue--> \n");

    pre_nextnv = null;
    Double val;
    for (int i = 0; i < elem_count; ++i) {
      first_elem = null;
      pre_elem = null;
      for (int v = 0; v < 3; ++v) {
        elem = DurableSinglyLinkedListFactory.create(m_act, elem_efproxies, elem_gftypes, false);
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

      nextnv = DurableSinglyLinkedListFactory.create(m_act, linkedefproxies, linkedgftypes, false);
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

    DurableSinglyLinkedList<DurableSinglyLinkedList<Double>> linkedvals = DurableSinglyLinkedListFactory.restore(m_act,
        linkedefproxies, linkedgftypes, handler, false);
    Iterator<DurableSinglyLinkedList<Double>> iter = linkedvals.iterator();
    Iterator<Double> elemiter = null;

    System.out.printf(" Stage 2 -testLinkedNodeValueWithLinkedNodeValue--> \n");
    while (iter.hasNext()) {
      elemiter = iter.next().iterator();
      while (elemiter.hasNext()) {
        System.out.printf("%f ", elemiter.next());
      }
      System.out.printf(" Fetched an item... \n");
    }

    // Assert.assert, expected);(plist, plist2);

  }

}
