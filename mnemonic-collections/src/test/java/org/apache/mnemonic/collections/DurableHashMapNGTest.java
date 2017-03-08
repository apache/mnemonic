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

import org.apache.mnemonic.Utils;
import org.apache.mnemonic.RestorableAllocator;
import org.apache.mnemonic.NonVolatileMemAllocator;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.Reclaim;
import org.apache.mnemonic.Durable;
import org.apache.commons.lang3.tuple.Pair;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.AssertJUnit;

/**
 *
 *
 */

public class DurableHashMapNGTest {
  private long cKEYCAPACITY;
  private NonVolatileMemAllocator m_act;
  private long mInitialCapacity = 1;

  @BeforeClass
  public void setUp() {
    m_act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService("pmalloc"), 1024 * 1024 * 1024,
        "./pobj_hashmaps.dat", true);
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
  }

  @Test(enabled = true)
  public void testGetPutKeyDurable() {
    
    DurableType gtypes[] = {DurableType.DURABLE, DurableType.STRING};
    EntityFactoryProxy efproxies[] = {new EntityFactoryProxy() {
      @Override
      public <A extends RestorableAllocator<A>> Person<Long> restore(
          A allocator, EntityFactoryProxy[] factoryproxys,
          DurableType[] gfields, long phandler, boolean autoreclaim) {
        return PersonFactory.restore(allocator, factoryproxys, gfields, phandler, autoreclaim);
      }
      @Override
      public <A extends RestorableAllocator<A>> Person<Long> create(
          A allocator, EntityFactoryProxy[] factoryproxys,
          DurableType[] gfields, boolean autoreclaim) {
        return PersonFactory.create(allocator, factoryproxys, gfields, autoreclaim);
      }
    } };
    
    Person<Long> person =  (Person<Long>) efproxies[0].create(m_act, null, null, false);
    person.setAge((short) 31);
    person.setName("Bob", true);

    Person<Long> anotherPerson =  (Person<Long>) efproxies[0].create(m_act, null, null, false);
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
  }

  @Test(enabled = true)
  public void testGetPutValueDurable() {
    
    DurableType gtypes[] = {DurableType.STRING, DurableType.DURABLE};
    EntityFactoryProxy efproxies[] = {null, new EntityFactoryProxy() {
      @Override
      public <A extends RestorableAllocator<A>> Person<Long> restore(
          A allocator, EntityFactoryProxy[] factoryproxys,
          DurableType[] gfields, long phandler, boolean autoreclaim) {
        return PersonFactory.restore(allocator, factoryproxys, gfields, phandler, autoreclaim);
      }
      @Override
      public <A extends RestorableAllocator<A>> Person<Long> create(
          A allocator, EntityFactoryProxy[] factoryproxys,
          DurableType[] gfields, boolean autoreclaim) {
        return PersonFactory.create(allocator, factoryproxys, gfields, autoreclaim);
      }
    } };
    
    Person<Long> person =  (Person<Long>) efproxies[1].create(m_act, null, null, false);
    person.setAge((short) 31);
    DurableHashMap<String, Person<Long>> map = DurableHashMapFactory.create(m_act, 
                            efproxies, gtypes, mInitialCapacity, false);
    map.put("hello", person);
    
    Person<Long> per = map.get("hello");
    AssertJUnit.assertEquals(31, (int)per.getAge()); 
  }

  @Test(enabled = true)
  public void testGetPutKeyValueDurable() {
    
    DurableType gtypes[] = {DurableType.DURABLE, DurableType.DURABLE};
    EntityFactoryProxy efproxies[] = {new EntityFactoryProxy() {
      @Override
      public <A extends RestorableAllocator<A>> Person<Long> restore(
          A allocator, EntityFactoryProxy[] factoryproxys,
          DurableType[] gfields, long phandler, boolean autoreclaim) {
        return PersonFactory.restore(allocator, factoryproxys, gfields, phandler, autoreclaim);
      }
      @Override
      public <A extends RestorableAllocator<A>> Person<Long> create(
          A allocator, EntityFactoryProxy[] factoryproxys,
          DurableType[] gfields, boolean autoreclaim) {
        return PersonFactory.create(allocator, factoryproxys, gfields, autoreclaim);
      }
    }, new EntityFactoryProxy() {
      @Override
      public <A extends RestorableAllocator<A>> Person<Long> restore(
          A allocator, EntityFactoryProxy[] factoryproxys,
          DurableType[] gfields, long phandler, boolean autoreclaim) {
        return PersonFactory.restore(allocator, factoryproxys, gfields, phandler, autoreclaim);
      }
      @Override
      public <A extends RestorableAllocator<A>> Person<Long> create(
          A allocator, EntityFactoryProxy[] factoryproxys,
          DurableType[] gfields, boolean autoreclaim) {
        return PersonFactory.create(allocator, factoryproxys, gfields, autoreclaim);
      }
    } };
   
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
  }

  @Test(enabled = true)
  public void testGetPutMapOfMapDurable() {
    DurableType gtypes[] = {DurableType.STRING, DurableType.DURABLE};
    EntityFactoryProxy efproxies[] = {null, new EntityFactoryProxy() {
      @Override
      public <A extends RestorableAllocator<A>> Person<Long> restore(
          A allocator, EntityFactoryProxy[] factoryproxys,
          DurableType[] gfields, long phandler, boolean autoreclaim) {
        return PersonFactory.restore(allocator, factoryproxys, gfields, phandler, autoreclaim);
      }
      @Override
      public <A extends RestorableAllocator<A>> Person<Long> create(
          A allocator, EntityFactoryProxy[] factoryproxys,
          DurableType[] gfields, boolean autoreclaim) {
        return PersonFactory.create(allocator, factoryproxys, gfields, autoreclaim);
      }
    } };
    DurableType mapgtypes[] = {DurableType.STRING, DurableType.DURABLE, DurableType.STRING, DurableType.DURABLE};
    EntityFactoryProxy mapefproxies[] = {null, new EntityFactoryProxy() {
      @Override
      public <A extends RestorableAllocator<A>> Durable restore(
          A allocator, EntityFactoryProxy[] factoryproxys,
          DurableType[] gfields, long phandler, boolean autoreclaim) {
        Pair<DurableType[], EntityFactoryProxy[]> dpt = Utils.shiftDurableParams(gfields, factoryproxys, 2);
        return DurableHashMapFactory.restore(allocator, dpt.getRight(), dpt.getLeft(), phandler, autoreclaim);
      }
      @Override
      public <A extends RestorableAllocator<A>> Durable create(
          A allocator, EntityFactoryProxy[] factoryproxys,
          DurableType[] gfields, boolean autoreclaim) {
        Pair<DurableType[], EntityFactoryProxy[]> dpt = Utils.shiftDurableParams(gfields, factoryproxys, 2);
        return DurableHashMapFactory.create(allocator, dpt.getRight(), dpt.getLeft(), mInitialCapacity, autoreclaim);
      }
    }, null, new EntityFactoryProxy() {
      @Override
      public <A extends RestorableAllocator<A>> Person<Long> restore(
          A allocator, EntityFactoryProxy[] factoryproxys,
          DurableType[] gfields, long phandler, boolean autoreclaim) {
        return PersonFactory.restore(allocator, factoryproxys, gfields, phandler, autoreclaim);
      }
      @Override
      public <A extends RestorableAllocator<A>> Person<Long> create(
          A allocator, EntityFactoryProxy[] factoryproxys,
          DurableType[] gfields, boolean autoreclaim) {
        return PersonFactory.create(allocator, factoryproxys, gfields, autoreclaim);
      }
    } };
    
    Person<Long> person =  PersonFactory.create(m_act, null, null, false);
    person.setAge((short) 31);
    person.setName("Bob", true);


    Person<Long> anotherPerson =  (Person<Long>) efproxies[1].create(m_act, null, null, false);
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
  }
}
