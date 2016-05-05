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

import org.apache.mnemonic.NonVolatileMemAllocator;
import org.apache.mnemonic.OutOfHybridMemory;
import org.apache.mnemonic.Reclaim;
import org.apache.mnemonic.RetrieveDurableEntityError;
import org.apache.mnemonic.Utils;
import org.testng.annotations.Test;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.UUID;

/**
 *
 *
 */

public class DurablePersonNGTest {
  private long cKEYCAPACITY;

  @Test(expectedExceptions = { OutOfHybridMemory.class })
  public void testGenPeople() throws OutOfHybridMemory, RetrieveDurableEntityError {
    Random rand = Utils.createRandom();
    NonVolatileMemAllocator act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService("pmalloc"),
        1024 * 1024 * 8, "./pobj_person.dat", true);
    cKEYCAPACITY = act.handlerCapacity();
    act.setBufferReclaimer(new Reclaim<ByteBuffer>() {
      @Override
      public boolean reclaim(ByteBuffer mres, Long sz) {
        System.out.println(String.format("Reclaim Memory Buffer: %X  Size: %s", System.identityHashCode(mres),
            null == sz ? "NULL" : sz.toString()));
        return false;
      }
    });
    act.setChunkReclaimer(new Reclaim<Long>() {
      @Override
      public boolean reclaim(Long mres, Long sz) {
        System.out.println(String.format("Reclaim Memory Chunk: %X  Size: %s", System.identityHashCode(mres),
            null == sz ? "NULL" : sz.toString()));
        return false;
      }
    });

    for (long i = 0; i < cKEYCAPACITY; ++i) {
      act.setHandler(i, 0L);
    }

    Person<Integer> mother;
    Person<Integer> person;

    long keyidx = 0;
    long val;

    try {
      while (true) {
        // if (keyidx >= KEYCAPACITY) break;

        keyidx %= cKEYCAPACITY;

        System.out.printf("************ Generating People on Key %d ***********\n", keyidx);

        val = act.getHandler(keyidx);
        if (0L != val) {
          PersonFactory.restore(act, val, true);
        }

        person = PersonFactory.create(act);
        person.setAge((short) rand.nextInt(50));
        person.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);
        person.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);
        person.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);
        person.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);

        act.setHandler(keyidx, person.getHandler());

        for (int deep = 0; deep < rand.nextInt(100); ++deep) {

          mother = PersonFactory.create(act);
          mother.setAge((short) (50 + rand.nextInt(50)));
          mother.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);

          person.setMother(mother, true);

          person = mother;

        }
        ++keyidx;
      }
    } finally {
      act.close();
    }
  }

  @Test(dependsOnMethods = { "testGenPeople" })
  public void testCheckPeople() throws RetrieveDurableEntityError {
    NonVolatileMemAllocator act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService("pmalloc"),
        1024 * 1024 * 8, "./pobj_person.dat", true);
    act.setBufferReclaimer(new Reclaim<ByteBuffer>() {
      @Override
      public boolean reclaim(ByteBuffer mres, Long sz) {
        System.out.println(String.format("Reclaim Memory Buffer: %X  Size: %s", System.identityHashCode(mres),
            null == sz ? "NULL" : sz.toString()));
        return false;
      }
    });
    act.setChunkReclaimer(new Reclaim<Long>() {
      @Override
      public boolean reclaim(Long mres, Long sz) {
        System.out.println(String.format("Reclaim Memory Chunk: %X  Size: %s", System.identityHashCode(mres),
            null == sz ? "NULL" : sz.toString()));
        return false;
      }
    });

    long val;
    for (long i = 0; i < cKEYCAPACITY; ++i) {
      System.out.printf("----------Key %d--------------\n", i);
      val = act.getHandler(i);
      if (0L == val) {
        break;
      }
      Person<Integer> person = PersonFactory.restore(act, val, true);
      while (null != person) {
        person.testOutput();
        person = person.getMother();
      }
    }

    act.close();
  }
}
