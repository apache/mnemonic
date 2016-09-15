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

package org.apache.mnemonic;

/**
 *
 *
 */

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.Test;

public class DurablePersonBufferNGTest {
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
        System.out.println(String.format("WLReclaim Memory Buffer: %X  Size: %s", System.identityHashCode(mres),
            null == sz ? "NULL" : sz.toString()));
        return false;
      }
    });
    act.setChunkReclaimer(new Reclaim<Long>() {
      @Override
      public boolean reclaim(Long mres, Long sz) {
        System.out.println(String.format("WLReclaim Memory Chunk: %X  Size: %s", System.identityHashCode(mres),
            null == sz ? "NULL" : sz.toString()));
        return false;
      }
    });

    for (long i = 0; i < cKEYCAPACITY; ++i) {
      act.setHandler(i, 0L);
    }

    PersonBuffer<Integer> mother;
    PersonBuffer<Integer> person;

    int size = 1024;
    ByteBuffer bbuf = ByteBuffer.allocate(10);

    MemBufferHolder<?> mbh;
    mbh = act.createBuffer(size);
//    MemBufferHolder mbh = new MemBufferHolder(act, bbuf);
   
    Assert.assertNotNull(mbh);
    for (int i = 0; i < size; i++) {
      mbh.get().put((byte) rand.nextInt(16));
    }
    // if (bb.hasArray()) randomGenerator.nextBytes(bb.array());
    Assert.assertNotNull(mbh);
    Assert.assertEquals(size, mbh.get().capacity());
    System.out.println(String.format("[Seq.....] size %d - %d, (%s)", size, mbh.get().capacity(),
size == mbh.get().capacity() ? "Correct" : "Failed!!!"));

    long keyidx = 0;
    long val;

    try {
      while (true) {
        // if (keyidx >= KEYCAPACITY) break;

        keyidx %= cKEYCAPACITY;

        System.out.printf("************ Generating People on Key %d ***********\n", keyidx);

        val = act.getHandler(keyidx);
        if (0L != val) {
          PersonBufferFactory.restore(act, val, true);
        }

        person = PersonBufferFactory.create(act);
        person.setAge((short) rand.nextInt(50));
        person.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);
        person.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);
        person.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);
        person.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);
        person.setName(String.format("Name: [%d]", bbuf.capacity()), true);

        Assert.assertNotNull(mbh);
        Assert.assertNotNull(person);
        person.setNamebuffer(mbh, false);

        act.setHandler(keyidx, person.getHandler());

        for (int deep = 0; deep < rand.nextInt(100); ++deep) {

          mother = PersonBufferFactory.create(act);
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
        System.out.println(String.format("WMReclaim Memory Buffer: %X  Size: %s", System.identityHashCode(mres),
            null == sz ? "NULL" : sz.toString()));
        return false;
      }
    });
    act.setChunkReclaimer(new Reclaim<Long>() {
      @Override
      public boolean reclaim(Long mres, Long sz) {
        System.out.println(String.format("WMReclaim Memory Chunk: %X  Size: %s", System.identityHashCode(mres),
            null == sz ? "NULL" : sz.toString()));
        return false;
      }
    });

    long val;
    for (long i = 0; i < cKEYCAPACITY; ++i) {
      System.out.printf("----------Key WM %d--------------\n", i);
      val = act.getHandler(i);
      if (0L == val) {
        break;
      }
      PersonBuffer<Integer> person = PersonBufferFactory.restore(act, val, true);
      while (null != person) {
        person.testOutput();
        person = person.getMother();
      }
    }

    act.close();
  }
}
