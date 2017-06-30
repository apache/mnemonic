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

import org.testng.annotations.Test;

import java.util.Random;

/**
 * test the functionality of VolatileMemAllocator class.
 * 
 */
public class VolatileMemAllocatorNGTest {
  /**
   * test to allocate MemBufferHolder objects and then verify them.
   */
  @Test
  public void testMemByteBuffer() {
    Random randomGenerator = new Random();
    Allocator<VolatileMemAllocator> act = new VolatileMemAllocator(Utils.getVolatileMemoryAllocatorService("vmem"),
        1024 * 1024 * 1024, ".");
    MemBufferHolder<?> mbh;
    for (int idx = 1; idx <= 5; ++idx) {
      int size = randomGenerator.nextInt(1024 * 1024) + 1024 * 1024;
      mbh = act.createBuffer(size);
      for (int i = 0; i < size; i++) {
        mbh.get().put((byte) randomGenerator.nextInt(255));
      }
      // if (bb.hasArray()) randomGenerator.nextBytes(bb.array());
      System.out.println(String.format("[Seq.%d] size %d - %d, (%s)", idx, size, mbh.get().capacity(),
          size == mbh.get().capacity() ? "Correct" : "Failed!!!"));
      // mbh.destroy();
    }
  }

  /**
   * test to allocate MemChunkHolder objects and then verify them.
   */
  @Test
  public void testMemChunk() {
    Random randomGenerator = new Random();
    Allocator<VolatileMemAllocator> act = new VolatileMemAllocator(Utils.getVolatileMemoryAllocatorService("vmem"),
        1024 * 1024 * 1024, ".");
    MemChunkHolder<?> mch;
    for (int idx = 1; idx <= 5; ++idx) {
      int size = randomGenerator.nextInt(1024 * 1024) + 1024 * 1024;
      mch = act.createChunk(size);
      System.out.println(String.format("[Seq.%d] addr : %X", idx, size, mch.get()));
      mch.destroy();
    }
  }

}
