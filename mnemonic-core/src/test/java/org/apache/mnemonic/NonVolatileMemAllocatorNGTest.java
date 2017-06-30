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

import java.nio.ByteBuffer;
import java.util.Random;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 */

public class NonVolatileMemAllocatorNGTest {
  @Test
  public void testPMemByteBuffer() {
    Random randomGenerator = new Random();
    NonVolatileMemAllocator act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService("pmalloc"),
        1024 * 1024 * 1024, "./pmtest.dat", true);
    act.setBufferReclaimer(new Reclaim<ByteBuffer>() {
      @Override
      public boolean reclaim(ByteBuffer mres, Long sz) {
        System.out.println(String.format("Reclaim Memory Buffer: %X  Size: %s", System.identityHashCode(mres),
            null == sz ? "NULL" : sz.toString()));
        return false;
      }
    });
    MemBufferHolder<?> mbh;
    for (int idx = 1; idx <= 500; ++idx) {
      int size = randomGenerator.nextInt(1024 * 1024) + 1024 * 1024;
      mbh = act.createBuffer(size);
      Assert.assertNotNull(mbh);
      for (int i = 0; i < size; i++) {
        mbh.get().put((byte) randomGenerator.nextInt(255));
      }
      // if (bb.hasArray()) randomGenerator.nextBytes(bb.array());
      Assert.assertEquals(size, mbh.get().capacity());
      System.out.println(String.format("[Seq.%d] size %d - %d, (%s)", idx, size, mbh.get().capacity(),
          size == mbh.get().capacity() ? "Correct" : "Failed!!!"));
      // mbh.destroy();
    }
    act.close();
  }

  @Test
  public void testGetBufferAddress() {
    NonVolatileMemAllocator act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService("pmalloc"),
        1024 * 1024 * 1024, "./pmtest_buffer.dat", true);
    MemBufferHolder<NonVolatileMemAllocator> mbh;
    mbh = act.createBuffer(20000);
    long phandler = act.getBufferHandler(mbh);
    System.out.println(String.format("**** 0x%X", phandler));
    act.close();
  }

  @Test
  public void testGenPMemByteBufferWithKey() {
    Random randomGenerator = Utils.createRandom();
    NonVolatileMemAllocator act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService("pmalloc"),
        1024 * 1024 * 1024, "./pmtest_key.dat", true);
    act.setBufferReclaimer(new Reclaim<ByteBuffer>() {
      @Override
      public boolean reclaim(ByteBuffer mres, Long sz) {
        System.out.println(String.format("Reclaim Memory Buffer: %X  Size: %s", System.identityHashCode(mres),
            null == sz ? "NULL" : sz.toString()));
        return false;
      }
    });
    MemBufferHolder<NonVolatileMemAllocator> mbh;
    Long phandler;
    long keycount = act.handlerCapacity();
    for (int idx = 0; idx < keycount; ++idx) {
      int size = randomGenerator.nextInt(1024 * 1024) + 1024 * 1024;
      mbh = act.createBuffer(size);
      if (6 == idx) {
        size += 2000;
        mbh = mbh.resize(size);
      }
      Assert.assertNotNull(mbh);
      mbh.get().putInt(size);
      Assert.assertEquals(size, mbh.get().capacity());
      System.out.println(String.format("Generating PKey Value [Seq.%d] size %d - %d, (%s)", idx, size,
          mbh.get().capacity(), size == mbh.get().capacity() ? "Correct" : "Failed!!!"));
      phandler = act.getBufferHandler(mbh);
      System.out.println(String.format("---- 0x%X", phandler));
      act.setHandler(idx, phandler);
      mbh.cancelAutoReclaim();
    }
    act.close();
  }

  @Test(dependsOnMethods = { "testGenPMemByteBufferWithKey" })
  public void testCheckPMemByteBufferWithKey() {
    NonVolatileMemAllocator act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService("pmalloc"),
        1024 * 1024 * 1024, "./pmtest_key.dat", false);
    act.setBufferReclaimer(new Reclaim<ByteBuffer>() {
      @Override
      public boolean reclaim(ByteBuffer mres, Long sz) {
        System.out.println(String.format("Reclaim Memory Buffer: %X  Size: %s", System.identityHashCode(mres),
            null == sz ? "NULL" : sz.toString()));
        return false;
      }
    });
    MemBufferHolder<NonVolatileMemAllocator> mbh;
    for (int idx = 0; idx < act.handlerCapacity(); ++idx) {
      long phandler = act.getHandler(idx);
      mbh = act.retrieveBuffer(phandler);
      Assert.assertNotNull(mbh);
      int val = mbh.get().getInt();
      Assert.assertEquals(val, mbh.get().capacity());
      System.out.println(String.format("Checking PKey Value [Seq.%d] size %d - %d, (%s)", idx, val,
          mbh.get().capacity(), val == mbh.get().capacity() ? "Correct" : "Failed!!!"));
    }
    act.close();
  }

}
