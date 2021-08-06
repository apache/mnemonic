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

import java.util.Iterator;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.zip.Checksum;

import org.apache.mnemonic.EntityFactoryProxyHelper;
import org.apache.mnemonic.Utils;
import org.apache.mnemonic.NonVolatileMemAllocator;
import org.apache.mnemonic.OutOfHybridMemory;
import org.apache.mnemonic.DurableBuffer;
import org.apache.mnemonic.DurableChunk;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.Reclaim;
import org.apache.commons.lang3.RandomUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import org.testng.Assert;

/**
 *
 *
 */

public class DurableHashSetNGTest {
  private long cKEYCAPACITY;
  private NonVolatileMemAllocator m_act;
  private Random rand;
  @SuppressWarnings({"restriction", "UseOfSunClasses"})
  private sun.misc.Unsafe unsafe;
  private long initialCapacity;

  protected DurableBuffer<NonVolatileMemAllocator>
      genuptBuffer(NonVolatileMemAllocator act, Checksum cs, int size) {
    DurableBuffer<NonVolatileMemAllocator> ret = null;
    ret = act.createBuffer(size, false);
    if (null == ret) {
      throw new OutOfHybridMemory("Create Durable Buffer Failed.");
    }
    ret.get().clear();
    byte[] rdbytes = RandomUtils.nextBytes(size);
    Assert.assertNotNull(rdbytes);
    ret.get().put(rdbytes);
    cs.update(rdbytes, 0, rdbytes.length);
    ret.get().clear();
    return ret;
  }

  protected DurableChunk<NonVolatileMemAllocator>
      genuptChunk(NonVolatileMemAllocator act, Checksum cs, long size) {
    DurableChunk<NonVolatileMemAllocator> ret = null;
    ret = act.createChunk(size, false);
    if (null == ret) {
      throw new OutOfHybridMemory("Create Durable Chunk Failed.");
    }
    byte b;
    for (int i = 0; i < ret.getSize(); ++i) {
      b = (byte) rand.nextInt(255);
      unsafe.putByte(ret.get() + i, b);
      cs.update(b);
    }
    return ret;
  }

  protected int genRandSize() {
    return rand.nextInt(1024 * 1024) + 1024 * 1024;
  }

  @BeforeClass
  public void setUp() throws Exception {
    rand = Utils.createRandom();
    unsafe = Utils.getUnsafe();
    m_act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService("pmalloc"), 1024 * 1024 * 1024,
        "./pobj_hashset.dat", true);
    cKEYCAPACITY = m_act.handlerCapacity();
    m_act.setBufferReclaimer(new Reclaim<ByteBuffer>() {
      @Override
      public boolean reclaim(ByteBuffer mres, Long sz) {
        System.out.println(String.format("Reclaim Memory Buffer: %X  Size: %s", System.identityHashCode(mres),
            null == sz ? "NULL" : sz.toString()));
        System.out.println(" String buffer " + mres.asCharBuffer().toString());
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

  @Test(enabled = true)
  public void testAddRemoveSetIntegers() {
    DurableType gtypes[] = {DurableType.INTEGER};
    DurableHashSet<Integer> set = DurableHashSetFactory.create(m_act, null, gtypes, initialCapacity, false);

    Long handler = set.getHandler();
    boolean val;
    for (int i = 0; i < 10; i++) {
      val = set.add(i);
      Assert.assertTrue(val);
    }

    for (int i = 0; i < 10; i++) {
      val = set.contains(i);
      Assert.assertTrue(val);
    }

    for (int i = 0; i < 10; i++) {
      val = set.remove(i);
      Assert.assertTrue(val);
    }

    for (int i = 0; i < 10; i++) {
      val = set.contains(i);
      Assert.assertFalse(val);
    }

    set.destroy();
  }

  @Test(enabled = true)
  public void testAddRemoveSetStrings() {
    DurableType gtypes[] = {DurableType.STRING};
    DurableHashSet<String> set = DurableHashSetFactory.create(m_act, null, gtypes, initialCapacity, false);

    Long handler = set.getHandler();
    boolean val;
    for (int i = 0; i < 10; i++) {
      val = set.add("str" + i);
      Assert.assertTrue(val);
    }

    for (int i = 0; i < 10; i++) {
      val = set.contains("str" + i);
      Assert.assertTrue(val);
    }

    for (int i = 0; i < 10; i++) {
      val = set.remove("str" + i);
      Assert.assertTrue(val);
    }

    for (int i = 0; i < 10; i++) {
      val = set.contains("str" + i);
      Assert.assertFalse(val);
    }

    set.destroy();
  }

  @Test(enabled = true)
  public void testAddRemoveDurable() throws NoSuchMethodException, ClassNotFoundException {
    DurableType gtypes[] = {DurableType.DURABLE};
    EntityFactoryProxy efproxies[] = {new EntityFactoryProxyHelper<Person>(Person.class)};

    Person<Long> person =  (Person<Long>) efproxies[0].create(m_act, null, null, false);
    person.setAge((short) 31);
    person.setName("Bob", true);

    Person<Long> anotherPerson =  (Person<Long>) efproxies[0].create(m_act, null, null, false);
    anotherPerson.setAge((short) 30);
    anotherPerson.setName("Alice", true);

    DurableHashSet<Person<Long>> set = DurableHashSetFactory.create(m_act, efproxies, gtypes, initialCapacity, false);
    boolean val = set.add(person);
    AssertJUnit.assertTrue(val);
    val = set.contains(person);
    AssertJUnit.assertTrue(val);
    val = set.contains(anotherPerson);
    AssertJUnit.assertFalse(val);
    val = set.add(anotherPerson);
    AssertJUnit.assertTrue(val);
    val = set.contains(anotherPerson);
    AssertJUnit.assertTrue(val);

    val = set.remove(person);
    AssertJUnit.assertTrue(val);
    val = set.contains(person);
    AssertJUnit.assertFalse(val);
    val = set.contains(anotherPerson);
    AssertJUnit.assertTrue(val);

    val = set.remove(anotherPerson);
    AssertJUnit.assertTrue(val);
    val = set.contains(anotherPerson);
    AssertJUnit.assertFalse(val);

    set.destroy();
  }

  @Test(enabled = true)
  public void testSetIterator() {
    DurableType gtypes[] = {DurableType.STRING};
    DurableHashSet<String> set = DurableHashSetFactory.create(m_act, null, gtypes, initialCapacity, false);

    Long handler = set.getHandler();
    set.add("hello");
    set.add("world");
    AssertJUnit.assertEquals(set.getSize(), 2);

    Iterator<String> iter = set.iterator();
    int count = 0;
    String entry = "";
    while (iter.hasNext()) {
      entry = iter.next();
      count++;
      if (entry.equals("world")) {
        iter.remove();
      }
    }
    AssertJUnit.assertEquals(count, 2);
    AssertJUnit.assertEquals(set.getSize(), 1);
    iter = set.iterator();
    count = 0;
    while (iter.hasNext()) {
      entry = iter.next();
      iter.remove();
      count++;
    }
    AssertJUnit.assertEquals(count, 1);
    AssertJUnit.assertEquals(set.getSize(), 0);
    AssertJUnit.assertEquals(entry, "hello");

    iter = set.iterator();
    count = 0;
    while (iter.hasNext()) {
      entry = iter.next();
      count++;
    }
    AssertJUnit.assertEquals(count, 0);
    set.add("hello");
    set.add("world");
    AssertJUnit.assertEquals(set.getSize(), 2);

    iter = set.iterator();
    count = 0;
    while (iter.hasNext()) {
      entry = iter.next();
      count++;
    }
    AssertJUnit.assertEquals(count, 2);

    set.destroy();
  }

}
