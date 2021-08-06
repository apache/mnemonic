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
import java.util.Random;
import java.util.zip.Checksum;
import java.util.zip.CRC32;
import java.util.Iterator;
import org.apache.commons.lang3.ArrayUtils;

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
import org.testng.Assert;

/**
 *
 *
 */

public class DurableArrayNGTest {
  private long cKEYCAPACITY;
  private NonVolatileMemAllocator m_act;
  private Random rand;
  @SuppressWarnings({"restriction", "UseOfSunClasses"})
  private sun.misc.Unsafe unsafe;

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
        "./pobj_array.dat", true);
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
  public void testGetSetArrayPrimitives() {
    DurableType gtypes[] = {DurableType.INTEGER};
    int capacity = 10;
    DurableArray<Integer> array = DurableArrayFactory.create(m_act, null, gtypes, capacity, false);

    Long handler = array.getHandler();
    for (int i = 0; i < capacity; i++) {
      array.set(i, 100 + i);
    }

    for (int i = 0; i < capacity; i++) {
      Assert.assertEquals(array.get(i).intValue(), 100 + i);
    }

    Iterator<Integer> itr = array.iterator();
    int val = 0;
    while (itr.hasNext()) {
     Assert.assertEquals(itr.next().intValue(), 100 + val);
     val++;
    }
    Assert.assertEquals(val, capacity);
    array.destroy();
  }


  @Test(enabled = true)
  public void testGetSetArrayString() {
    DurableType gtypes[] = {DurableType.STRING};
    int capacity = 10;
    DurableArray<String> array = DurableArrayFactory.create(m_act, null, gtypes, capacity, false);

    Long handler = array.getHandler();
    for (int i = 0; i < capacity; i++) {
      array.set(i, "string" + i);
    }

    for (int i = 0; i < capacity; i++) {
      Assert.assertEquals(array.get(i), "string" + i);
    }

    Iterator<String> itr = array.iterator();
    int val = 0;
    while (itr.hasNext()) {
     Assert.assertEquals(itr.next(), "string" + val);
     val++;
    }
    Assert.assertEquals(val, capacity);

    array.destroy();
  }

  @Test(enabled = true)
  public void testGetSetArrayBuffer() {
    DurableType gtypes[] = {DurableType.BUFFER};
    int capacity = 10;
    DurableArray<DurableBuffer> array = DurableArrayFactory.create(m_act, null, gtypes, capacity, false);

    Long handler = array.getHandler();
    long bufVal;

    Checksum bufferCheckSum = new CRC32();
    bufferCheckSum.reset();


    for (int i = 0; i < capacity; i++) {
      array.set(i, genuptBuffer(m_act, bufferCheckSum, genRandSize()));
    }
    bufVal = bufferCheckSum.getValue();

    bufferCheckSum.reset();

    for (int i = 0; i < capacity; i++) {
      DurableBuffer<NonVolatileMemAllocator> db = array.get(i);
      Assert.assertNotNull(db);
      byte buf[] = new byte[db.get().capacity()];
      db.get().get(buf);
      bufferCheckSum.update(buf, 0, buf.length);
      db.get().clear();
    }
    Assert.assertEquals(bufferCheckSum.getValue(), bufVal);
    bufferCheckSum.reset();

    DurableArray<DurableBuffer> restoredArray = DurableArrayFactory.restore(m_act, null, gtypes, handler, false);
    for (int i = 0; i < capacity; i++) {
      DurableBuffer<NonVolatileMemAllocator> db = restoredArray.get(i);
      Assert.assertNotNull(db);
      byte buf[] = new byte[db.get().capacity()];
      db.get().get(buf);
      bufferCheckSum.update(buf, 0, buf.length);
      db.get().clear();
    }
    Assert.assertEquals(bufferCheckSum.getValue(), bufVal);

    bufferCheckSum.reset();
    Iterator<DurableBuffer> itr = restoredArray.iterator();
    int val = 0;
    while (itr.hasNext()) {
      DurableBuffer<NonVolatileMemAllocator> db = itr.next();
      Assert.assertNotNull(db);
      byte buf[] = new byte[db.get().capacity()];
      db.get().get(buf);
      bufferCheckSum.update(buf, 0, buf.length);
      db.get().clear();
      val++;
    }
    Assert.assertEquals(val, capacity);
    Assert.assertEquals(bufferCheckSum.getValue(), bufVal);

    restoredArray.destroy();
  }

  @Test(enabled = true)
  public void testGetSetArrayChunk() {
    DurableType gtypes[] = {DurableType.CHUNK};
    int capacity = 10;
    DurableArray<DurableChunk> array = DurableArrayFactory.create(m_act, null, gtypes, capacity, false);

    Long handler = array.getHandler();

    long chunkVal;

    Checksum chunkCheckSum = new CRC32();
    chunkCheckSum.reset();


    for (int i = 0; i < capacity; i++) {
      array.set(i, genuptChunk(m_act, chunkCheckSum, genRandSize()));
    }
    chunkVal = chunkCheckSum.getValue();
    chunkCheckSum.reset();

    for (int i = 0; i < capacity; i++) {
      DurableChunk<NonVolatileMemAllocator> dc = array.get(i);
      Assert.assertNotNull(dc);
      for (int j = 0; j < dc.getSize(); ++j) {
        byte b = unsafe.getByte(dc.get() + j);
        chunkCheckSum.update(b);
      }
    }
    Assert.assertEquals(chunkCheckSum.getValue(), chunkVal);
    chunkCheckSum.reset();

    DurableArray<DurableChunk> restoredArray = DurableArrayFactory.restore(m_act, null, gtypes, handler, false);
    for (int i = 0; i < capacity; i++) {
      DurableChunk<NonVolatileMemAllocator> dc = restoredArray.get(i);
      Assert.assertNotNull(dc);
      for (int j = 0; j < dc.getSize(); ++j) {
        byte b = unsafe.getByte(dc.get() + j);
        chunkCheckSum.update(b);
      }
    }
    Assert.assertEquals(chunkCheckSum.getValue(), chunkVal);

    chunkCheckSum.reset();
    Iterator<DurableChunk> itr = restoredArray.iterator();
    int val = 0;
    while (itr.hasNext()) {
      DurableChunk<NonVolatileMemAllocator> dc = itr.next();
      Assert.assertNotNull(dc);
      for (int j = 0; j < dc.getSize(); ++j) {
        byte b = unsafe.getByte(dc.get() + j);
        chunkCheckSum.update(b);
      }
      val++;
    }
    Assert.assertEquals(val, capacity);
    Assert.assertEquals(chunkCheckSum.getValue(), chunkVal);

    restoredArray.destroy();
  }

  @Test(enabled = true)
  public void testGetSetArrayDurable() throws NoSuchMethodException, ClassNotFoundException {
    DurableType gtypes[] = {DurableType.DURABLE};
    EntityFactoryProxy efproxies[] = {new EntityFactoryProxyHelper<Person>(Person.class)};

    Person<Long> person =  (Person<Long>) efproxies[0].create(m_act, null, null, false);
    person.setName("Alice", false);
    person.setAge((short) 31);

    DurableArray<Person<Long>> array = DurableArrayFactory.create(m_act, efproxies, gtypes, 3, false);

    Long handler = array.getHandler();
    array.set(0, person);
    array.set(2, person);

    Assert.assertEquals(array.get(0).getAge().intValue(), 31);
    Assert.assertEquals(array.get(2).getName(), "Alice");
    Assert.assertNull(array.get(1));

    DurableArray<Person<Long>> restoredArray = DurableArrayFactory.restore(m_act, efproxies, gtypes, handler, false);

    Assert.assertEquals(restoredArray.get(0).getAge().intValue(), 31);
    Assert.assertEquals(restoredArray.get(2).getName(), "Alice");
    Assert.assertNull(array.get(1));

    restoredArray.destroy();
  }

  @Test(enabled = true)
  public void testGetSetArrayLinkedNodeInteger() throws NoSuchMethodException, ClassNotFoundException {
    int val = rand.nextInt();
    int capacity = 10;
    DurableType gtypes[] = {DurableType.INTEGER};
    SinglyLinkedNode<Integer> plln = SinglyLinkedNodeFactory.create(m_act, null, gtypes, false);
    plln.setItem(val, true);

    DurableType arraytypes[] = {DurableType.DURABLE, DurableType.INTEGER};
    EntityFactoryProxy arrayproxies[] = {
        new EntityFactoryProxyHelper<SinglyLinkedNode>(SinglyLinkedNode.class, 1)};
    DurableArray<SinglyLinkedNode<Integer>> array = DurableArrayFactory.create(m_act, arrayproxies,
                                        arraytypes, capacity, false);

    int index = rand.nextInt(capacity);
    Long handler = array.getHandler();
    array.set(index, plln);

    Assert.assertEquals(array.get(index).getItem().intValue(), val);
    DurableArray<SinglyLinkedNode<Integer>> restoredArray = DurableArrayFactory.restore(m_act, arrayproxies,
                                        arraytypes, handler, false);
    Assert.assertEquals(restoredArray.get(index).getItem().intValue(), val);
    restoredArray.destroy();
  }

  @Test(enabled = true)
  public void testGetSetArrayofMaps() throws NoSuchMethodException, ClassNotFoundException {
  DurableType gtypes[] = {DurableType.STRING, DurableType.DURABLE};
    EntityFactoryProxy efproxies[] = {null, new EntityFactoryProxyHelper<Person>(Person.class)};
    Person<Long> person =  (Person<Long>) efproxies[1].create(m_act, null, null, false);
    person.setName("Alice", false);
    person.setAge((short) 31);
    DurableHashMap<String, Person<Long>> first = DurableHashMapFactory.create(m_act,
                            efproxies, gtypes, 10, false);
    first.put("hello", person);
    Person<Long> anotherPerson =  (Person<Long>) efproxies[1].create(m_act, null, null, false);
    anotherPerson.setAge((short) 30);
    anotherPerson.setName("Bob", false);
    DurableHashMap<String, Person<Long>> second = DurableHashMapFactory.create(m_act,
                            efproxies, gtypes, 10, false);
    second.put("world", anotherPerson);


    DurableType arraytypes[] = {DurableType.DURABLE};
    arraytypes = ArrayUtils.addAll(arraytypes, gtypes);
    EntityFactoryProxy arrayproxies[] = {
        new EntityFactoryProxyHelper<DurableHashMap>(DurableHashMap.class, 1)};
    arrayproxies = ArrayUtils.addAll(arrayproxies, efproxies);

    DurableArray<DurableHashMap<String, Person<Long>>> array = DurableArrayFactory.create(m_act, arrayproxies,
                                        arraytypes, 3, false);

    Long handler = array.getHandler();
    array.set(0, first);
    array.set(2, second);

    Assert.assertEquals(array.get(0).get("hello").getName(), "Alice");
    Assert.assertEquals(array.get(2).get("world").getName(), "Bob");
    Assert.assertEquals(array.get(0).get("hello").getAge().intValue(), 31);
    Assert.assertEquals(array.get(2).get("world").getAge().intValue(), 30);
    Assert.assertNull(array.get(2).get("testing"));
    Assert.assertNull(array.get(1));

    DurableArray<DurableHashMap<String, Person<Long>>> restoredArray = DurableArrayFactory.restore(m_act, arrayproxies,
                                        arraytypes, handler, false);

    Assert.assertEquals(restoredArray.get(0).get("hello").getName(), "Alice");
    Assert.assertEquals(restoredArray.get(2).get("world").getName(), "Bob");
    Assert.assertEquals(restoredArray.get(0).get("hello").getAge().intValue(), 31);
    Assert.assertEquals(restoredArray.get(2).get("world").getAge().intValue(), 30);
    Assert.assertNull(restoredArray.get(2).get("testing"));
    Assert.assertNull(restoredArray.get(1));

    restoredArray.destroy();
  }
}
