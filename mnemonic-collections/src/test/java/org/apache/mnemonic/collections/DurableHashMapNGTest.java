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
import java.util.zip.CRC32;

import org.apache.mnemonic.EntityFactoryProxyHelper;
import org.apache.mnemonic.Utils;
import org.apache.mnemonic.NonVolatileMemAllocator;
import org.apache.mnemonic.OutOfHybridMemory;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.DurableBuffer;
import org.apache.mnemonic.DurableChunk;
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

public class DurableHashMapNGTest {
  private long cKEYCAPACITY;
  private NonVolatileMemAllocator m_act;
  private Random rand;
  @SuppressWarnings({"restriction", "UseOfSunClasses"})
  private sun.misc.Unsafe unsafe;
  private long mInitialCapacity = 1;

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
        "./pobj_hashmaps.dat", true);
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
  public void testGetPutRemovePrimitives() {
    DurableType gtypes[] = {DurableType.STRING, DurableType.INTEGER};
    DurableHashMap<String, Integer> map = DurableHashMapFactory.create(m_act, null, gtypes, mInitialCapacity, false);

    Long handler = map.getHandler();
    Integer val = map.put("hello", 1);
    AssertJUnit.assertNull(val);
    val = map.put("hello", 1);
    AssertJUnit.assertEquals(1, val.intValue());
    val = map.put("world", 2);
    AssertJUnit.assertNull(val);
    val = map.put("hello", 2);
    AssertJUnit.assertEquals(1, val.intValue());
    val = map.put("hello", 3);
    AssertJUnit.assertEquals(2, val.intValue());

    val = map.get("hello");
    AssertJUnit.assertEquals(3, val.intValue());
    val = map.get("world");
    AssertJUnit.assertEquals(2, val.intValue());
    val = map.get("test");
    AssertJUnit.assertNull(val);
    val = map.put("testing", 5);
    AssertJUnit.assertNull(val);
    val = map.get("testing");
    AssertJUnit.assertEquals(5, val.intValue());
    val = map.remove("testing");
    AssertJUnit.assertEquals(5, val.intValue());
    val = map.get("testing");
    AssertJUnit.assertNull(val);
    val = map.remove("testing");
    AssertJUnit.assertNull(val);
    val = map.remove("world");
    AssertJUnit.assertEquals(2, val.intValue());

    Iterator<MapEntry<String, Integer>> iter = map.iterator();
    MapEntry<String, Integer> entry = null;
    int count = 0;
    while (iter.hasNext()) {
      entry = iter.next();
      count++;
    }
    AssertJUnit.assertEquals(count, 1);
    AssertJUnit.assertEquals(entry.getKey(), "hello");
    AssertJUnit.assertEquals(entry.getValue().intValue(), 3);

    DurableHashMap<String, Integer> restoredMap = DurableHashMapFactory.restore(m_act, null, gtypes, handler,
        false);
    val = restoredMap.get("hello");
    AssertJUnit.assertEquals(3, val.intValue());
    val = restoredMap.get("world");
    AssertJUnit.assertNull(val);
    val = restoredMap.get("test");
    AssertJUnit.assertNull(val);
    val = map.get("testing");
    AssertJUnit.assertNull(val);
    val = map.put("test", 4);
    AssertJUnit.assertNull(val);
    val = restoredMap.get("test");
    AssertJUnit.assertEquals(4, val.intValue());

    restoredMap.destroy();
  }

  @Test(enabled = true)
  public void testGetPutKeyDurable() throws NoSuchMethodException, ClassNotFoundException {

    DurableType gtypes[] = {DurableType.DURABLE, DurableType.STRING};
    EntityFactoryProxy efproxies[] = {new EntityFactoryProxyHelper<Person>(Person.class)};

    Person<Long> person =  (Person<Long>) efproxies[0].create(m_act, null, null, false);
    person.setAge((short) 31);
    person.setName("Bob", true);

    Person<Long> anotherPerson =
        (Person<Long>) efproxies[0].create(m_act, null, null, false);
    anotherPerson.setAge((short) 30);
    anotherPerson.setName("Alice", true);

    DurableHashMap<Person<Long>, String> map = DurableHashMapFactory.create(m_act,
                      efproxies, gtypes, mInitialCapacity, false);
    String str = map.put(person, "hello");
    AssertJUnit.assertNull(str);
    str = map.get(person);
    AssertJUnit.assertEquals(str, "hello");
    str = map.put(person, "world");
    AssertJUnit.assertEquals(str, "hello");
    str = map.get(person);
    AssertJUnit.assertEquals(str, "world");

    str = map.put(anotherPerson, "testing");
    AssertJUnit.assertNull(str);
    str = map.get(anotherPerson);
    AssertJUnit.assertEquals(str, "testing");
    str = map.get(person);
    AssertJUnit.assertEquals(str, "world");

    Person<Long> third =  (Person<Long>) efproxies[0].create(m_act, null, null, false);
    third.setAge((short) 31);
    third.setName("Bob", true);

    str = map.get(third);
    AssertJUnit.assertEquals(str, "world");
    third.destroy();
    map.destroy();
  }

  @Test(enabled = true)
  public void testGetPutValueDurable() throws NoSuchMethodException, ClassNotFoundException {

    DurableType gtypes[] = {DurableType.STRING, DurableType.DURABLE};
    EntityFactoryProxy efproxies[] = {null, new EntityFactoryProxyHelper<Person>(Person.class)};

    Person<Long> person =  (Person<Long>) efproxies[1].create(m_act, null, null, false);
    person.setName("Alice", false);
    person.setAge((short) 31);
    DurableHashMap<String, Person<Long>> map = DurableHashMapFactory.create(m_act,
                            efproxies, gtypes, mInitialCapacity, false);
    map.put("hello", person);
    Person<Long> anotherPerson =  (Person<Long>) efproxies[1].create(m_act, null, null, false);
    anotherPerson.setAge((short) 30);
    anotherPerson.setName("Bob", false);
    map.put("world", anotherPerson);

    Person<Long> per = map.get("hello");
    AssertJUnit.assertEquals(31, (int)per.getAge());

    per = map.get("world");
    AssertJUnit.assertEquals(30, (int)per.getAge());

    Person<Long> third =  (Person<Long>) efproxies[1].create(m_act, null, null, false);
    third.setAge((short) 29);
    third.setName("Frank", false);
    per = map.put("world", third);
    per.destroy();

    per = map.get("world");
    AssertJUnit.assertEquals(29, (int)per.getAge());
    map.destroy();
  }

  @Test(enabled = true)
  public void testGetPutKeyValueDurable() throws NoSuchMethodException, ClassNotFoundException {

    DurableType gtypes[] = {DurableType.DURABLE, DurableType.DURABLE};
    EntityFactoryProxy efproxies[] = {new EntityFactoryProxyHelper<Person>(Person.class),
        new EntityFactoryProxyHelper<Person>(Person.class)};

    Person<Long> person =  (Person<Long>) efproxies[0].create(m_act, null, null, false);
    person.setAge((short) 31);
    person.setName("Bob", true);

    Person<Long> anotherPerson =  (Person<Long>) efproxies[1].create(m_act, null, null, false);
    anotherPerson.setAge((short) 30);
    anotherPerson.setName("Alice", true);

    DurableHashMap<Person<Long>, Person<Long>> map = DurableHashMapFactory.create(m_act,
                            efproxies, gtypes, mInitialCapacity, false);
    map.put(person, anotherPerson);

    Person<Long> per = map.get(person);
    AssertJUnit.assertEquals(30, (int)per.getAge());
    per = map.get(anotherPerson);
    AssertJUnit.assertNull(per);

    map.destroy();
  }

  @Test(enabled = true)
  public void testGetPutMapOfMapDurable() throws NoSuchMethodException, ClassNotFoundException {
    DurableType gtypes[] = {DurableType.STRING, DurableType.DURABLE};
    EntityFactoryProxy efproxies[] = {null, new EntityFactoryProxyHelper<Person>(Person.class)};
    DurableType mapgtypes[] = {DurableType.STRING, DurableType.DURABLE, DurableType.STRING, DurableType.DURABLE};
    EntityFactoryProxy mapefproxies[] = {null,
        new EntityFactoryProxyHelper<DurableHashMap>(DurableHashMap.class, 2), null,
        new EntityFactoryProxyHelper<Person>(Person.class)};

    Person<Long> person =  PersonFactory.create(m_act, null, null, false);
    person.setAge((short) 31);
    person.setName("Bob", true);


    Person<Long> anotherPerson =  PersonFactory.create(m_act, null, null, false);
    anotherPerson.setAge((short) 30);
    anotherPerson.setName("Alice", true);

    DurableHashMap<String, Person<Long>> map = DurableHashMapFactory.create(m_act,
                            efproxies, gtypes, mInitialCapacity, false);
    map.put("world", person);
    Person<Long> per = map.get("world");
    AssertJUnit.assertEquals(31, (int)per.getAge());

    DurableHashMap<String, DurableHashMap<String, Person<Long>>> bigMap = DurableHashMapFactory.create(m_act,
                            mapefproxies, mapgtypes, mInitialCapacity, false);
    bigMap.put("hello", map);
    per = bigMap.get("hello").get("world");
    AssertJUnit.assertEquals(31, (int)per.getAge());

    bigMap.get("hello").put("testing", anotherPerson);
    per = bigMap.get("hello").get("testing");
    AssertJUnit.assertEquals("Alice", per.getName());

    bigMap.destroy();
  }

  @Test(enabled = true)
  public void testAutoResizeMaps() {
    DurableType gtypes[] = {DurableType.STRING, DurableType.INTEGER};
    DurableHashMap<String, Integer> map = DurableHashMapFactory.create(m_act, null, gtypes, mInitialCapacity, false);
    Long handler = map.getHandler();
    Iterator<MapEntry<String, Integer>> iter = null;
    MapEntry<String, Integer> entry = null;
    int count = 0;
    Integer val = 0;
    for (int i = 0; i < 200; i++) {
      val = map.put("str" + i, i);
      AssertJUnit.assertNull(val);
    }

    iter = map.iterator();
    while (iter.hasNext()) {
      entry = iter.next();
      count++;
    }
    AssertJUnit.assertEquals(count, 200);
    AssertJUnit.assertEquals(map.getSize(), 200);
    for (int i = 0; i < 200; i++) {
      AssertJUnit.assertEquals(map.get("str" + i).intValue(), i);
    }
    DurableHashMap<String, Integer> restoredMap = DurableHashMapFactory.restore(m_act, null, gtypes, handler,
        false);
    AssertJUnit.assertEquals(restoredMap.getSize(), 200);
    for (int i = 0; i < 200; i++) {
      AssertJUnit.assertEquals(restoredMap.get("str" + i).intValue(), i);
    }

    iter = restoredMap.iterator();
    count = 0;
    while (iter.hasNext()) {
      entry = iter.next();
      count++;
    }
    AssertJUnit.assertEquals(count, 200);

    for (int i = 0; i < 100; i++) {
      AssertJUnit.assertEquals(restoredMap.remove("str" + i).intValue(), i);
    }
    AssertJUnit.assertEquals(restoredMap.getSize(), 100);
    for (int i = 0; i < 200; i++) {
      if (i < 100) {
        AssertJUnit.assertNull(restoredMap.get("str" + i));
      } else {
      AssertJUnit.assertEquals(restoredMap.get("str" + i).intValue(), i);
      }
    }

    iter = restoredMap.iterator();
    count = 0;
    while (iter.hasNext()) {
      entry = iter.next();
      count++;
    }
    AssertJUnit.assertEquals(count, 100);

    iter = restoredMap.iterator();
    count = 0;
    while (iter.hasNext()) {
      entry = iter.next();
      count++;
      if (count > 50) {
        iter.remove();
      }
    }
    AssertJUnit.assertEquals(restoredMap.getSize(), 50);

    iter = restoredMap.iterator();
    count = 0;
    while (iter.hasNext()) {
      entry = iter.next();
      count++;
    }
    AssertJUnit.assertEquals(count, 50);

    restoredMap.destroy();
  }

  @Test(enabled = true)
  public void testMapIterator() {
    DurableType gtypes[] = {DurableType.STRING, DurableType.INTEGER};
    DurableHashMap<String, Integer> map = DurableHashMapFactory.create(m_act, null, gtypes, mInitialCapacity, false);

    Long handler = map.getHandler();
    map.put("hello", 1);
    map.put("world", 2);
    AssertJUnit.assertEquals(map.getSize(), 2);

    Iterator<MapEntry<String, Integer>> iter = map.iterator();
    MapEntry<String, Integer> entry = null;
    int count = 0;
    while (iter.hasNext()) {
      entry = iter.next();
      count++;
      if (entry.getKey().equals("world")) {
        iter.remove();
      }
    }
    AssertJUnit.assertEquals(count, 2);
    AssertJUnit.assertEquals(map.getSize(), 1);
    iter = map.iterator();
    count = 0;
    while (iter.hasNext()) {
      entry = iter.next();
      iter.remove();
      count++;
    }
    AssertJUnit.assertEquals(count, 1);
    AssertJUnit.assertEquals(map.getSize(), 0);
    AssertJUnit.assertEquals(entry.getKey(), "hello");
    AssertJUnit.assertEquals(entry.getValue().intValue(), 1);

    iter = map.iterator();
    count = 0;
    while (iter.hasNext()) {
      entry = iter.next();
      count++;
    }
    AssertJUnit.assertEquals(count, 0);
    map.put("hello", 1);
    map.put("world", 2);
    AssertJUnit.assertEquals(map.get("hello").intValue(), 1);
    AssertJUnit.assertEquals(map.get("world").intValue(), 2);
    AssertJUnit.assertEquals(map.getSize(), 2);

    iter = map.iterator();
    count = 0;
    while (iter.hasNext()) {
      entry = iter.next();
      count++;
    }
    AssertJUnit.assertEquals(count, 2);

    map.destroy();
  }

  @Test(enabled = true)
  public void testMapValueBuffer() {
    DurableType gtypes[] = {DurableType.STRING, DurableType.BUFFER};
    DurableHashMap<String, DurableBuffer> map = DurableHashMapFactory.create(m_act, null, gtypes, 1, false);
    long bufVal;

    Checksum bufferCheckSum = new CRC32();
    bufferCheckSum.reset();

    Long handler = map.getHandler();
    for (int i = 0; i < 10; i++) {
      map.put("buffer" + i, genuptBuffer(m_act, bufferCheckSum, genRandSize()));
    }

    bufVal = bufferCheckSum.getValue();

    bufferCheckSum.reset();
    for (int i = 0; i < 10; i++) {
      DurableBuffer<NonVolatileMemAllocator> db = map.get("buffer" + i);
      Assert.assertNotNull(db);
      byte buf[] = new byte[db.get().capacity()];
      db.get().get(buf);
      bufferCheckSum.update(buf, 0, buf.length);
    }
    Assert.assertEquals(bufferCheckSum.getValue(), bufVal);

    bufferCheckSum.reset();
    DurableHashMap<String, DurableBuffer> restoredMap = DurableHashMapFactory.restore(m_act,
                            null, gtypes, handler, false);
    for (int i = 0; i < 10; i++) {
      DurableBuffer<NonVolatileMemAllocator> db = restoredMap.get("buffer" + i);
      Assert.assertNotNull(db);
      byte buf[] = new byte[db.get().capacity()];
      db.get().get(buf);
      bufferCheckSum.update(buf, 0, buf.length);
    }
    Assert.assertEquals(bufferCheckSum.getValue(), bufVal);

    restoredMap.destroy();
  }

  @Test(enabled = true)
  public void testMapValueChunk() {
    DurableType gtypes[] = {DurableType.STRING, DurableType.CHUNK};
    DurableHashMap<String, DurableChunk> map = DurableHashMapFactory.create(m_act, null, gtypes, 1, false);
    long chunkVal;

    Checksum chunkCheckSum = new CRC32();
    chunkCheckSum.reset();

    Long handler = map.getHandler();
    for (int i = 0; i < 10; i++) {
      map.put("chunk" + i, genuptChunk(m_act, chunkCheckSum, genRandSize()));
    }

    chunkVal = chunkCheckSum.getValue();
    chunkCheckSum.reset();

    for (int i = 0; i < 10; i++) {
      DurableChunk<NonVolatileMemAllocator> dc = map.get("chunk" + i);
      for (int j = 0; j < dc.getSize(); ++j) {
        byte b = unsafe.getByte(dc.get() + j);
        chunkCheckSum.update(b);
      }
    }
    chunkVal = chunkCheckSum.getValue();
    Assert.assertEquals(chunkCheckSum.getValue(), chunkVal);

    chunkCheckSum.reset();
    DurableHashMap<String, DurableChunk> restoredMap = DurableHashMapFactory.restore(m_act,
                            null, gtypes, handler, false);

    for (int i = 0; i < 10; i++) {
      DurableChunk<NonVolatileMemAllocator> dc = restoredMap.get("chunk" + i);
      for (int j = 0; j < dc.getSize(); ++j) {
        byte b = unsafe.getByte(dc.get() + j);
        chunkCheckSum.update(b);
      }
    }
    chunkVal = chunkCheckSum.getValue();
    Assert.assertEquals(chunkCheckSum.getValue(), chunkVal);

    restoredMap.destroy();
  }
}
