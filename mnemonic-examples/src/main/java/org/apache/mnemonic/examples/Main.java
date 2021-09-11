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

package org.apache.mnemonic.examples;

import java.nio.ByteBuffer;
import java.util.Random;

import org.apache.mnemonic.VolatileMemAllocator;
import org.apache.mnemonic.CommonAllocator;
import org.apache.mnemonic.MemBufferHolder;
import org.apache.mnemonic.MemChunkHolder;
import org.apache.mnemonic.MemClustering;
import org.apache.mnemonic.Reclaim;
import org.apache.mnemonic.SysMemAllocator;
import org.apache.mnemonic.Utils;

/**
 * Main is the class of example.
 *
 */
@SuppressWarnings({"restriction", "unchecked"})
public class Main {

  /**
   * Run a example code to demonstrate some basic functionalities.
   *
   * @param argv
   *          array of commandline parameters
   */
  public static void main(String[] argv) throws Exception {
    Random randomGenerator = new Random();

    /* Generate configuration for each node */
    /*
     * Currently only one node is supported due to the external native code
     * 'vmem' returns null for multiple 'vmem' allocator instantialization
     */
    MemClustering.NodeConfig ncs[] = new MemClustering.NodeConfig[] {
        new MemClustering.NodeConfig(new SysMemAllocator(1024 * 1024 * 20, true).disableActiveGC(),
            MemClustering.PerformanceLevel.FASTEST),

        // new MemClustering.NodeConfig(new
        // BigDataMemAllocator(1024*1024*20, ".",
        // true).disableActiveGC(),
        // MemClustering.PerformanceLevel.FAST),
        // new MemClustering.NodeConfig(new
        // BigDataMemAllocator(1024*1024*20, ".",
        // true).disableActiveGC(),
        // MemClustering.PerformanceLevel.NORMAL),
        new MemClustering.NodeConfig(
            new VolatileMemAllocator(Utils.getVolatileMemoryAllocatorService("vmem"), 1024 * 1024 * 20, "."),
            // 1024 * 1024 * 20, "./example.dat", true),
            MemClustering.PerformanceLevel.SLOW), };

    for (MemClustering.NodeConfig nc : ncs) {
      /**
       * set a reclaimer for memory buffers
       */
      nc.getAllocator().setBufferReclaimer(new Reclaim<ByteBuffer>() {
        @Override
        public boolean reclaim(ByteBuffer mres, Long sz) {
          System.out.println(String.format("Reclaim Memory Buffer: %X  Size: %s", System.identityHashCode(mres),
              null == sz ? "NULL" : sz.toString()));
          return false;
        }
      });
      /**
       * set a reclaimer for memory chunks
       */
      nc.getAllocator().setChunkReclaimer(new Reclaim<Long>() {
        @Override
        public boolean reclaim(Long mres, Long sz) {
          System.out
              .println(String.format("Reclaim Memory Chunk: %X  Size: %s", mres, null == sz ? "NULL" : sz.toString()));
          return false;
        }
      });
    }

    /* Deploy Memory Clustering */
    MemClustering mclst = new MemClustering(ncs);

    /**
     * Set event callback for allocator changing. this callback is used to trace
     * the event of spilling out of allocation when previous allocator is unable
     * to meet the allocation requirement so trying to switch to next allocator.
     */
    mclst.setAllocatorChange(new MemClustering.AllocatorChange() {
      @Override
      public void changed(MemClustering.PerformanceLevel lvl, CommonAllocator prevallocator,
          CommonAllocator tgtallocator) {
        System.out.println(String.format("AllocatorChanged: %s, %X -> %X", lvl.name(),
            System.identityHashCode(prevallocator), System.identityHashCode(tgtallocator)));
      }
    });

    /**
     * Set event callback for performance level changing. this callback is used
     * to trace the event of downgrading to low level performance allocation.
     */
    mclst.setPerformanceLevelChange(new MemClustering.PerformanceLevelChange() {
      @Override
      public void changed(MemClustering.PerformanceLevel prevlvl, MemClustering.PerformanceLevel lvl) {
        System.out.println(
            String.format("PerfLevelChanged: %s -> %s", null == prevlvl ? "NULL" : prevlvl.name(), lvl.name()));
      }
    });

    /**
     * Start to create a piece of memory resource backed ByteBuffer and then
     * automatically release it or manually release it every six objects.
     */
    System.out.println(Utils.ANSI_GREEN + "[[Demo Allocation, Auto Destruction "
        + "and Manual Destruction of Big Memory ByteBuffer.]]" + Utils.ANSI_RESET);
    MemBufferHolder<?> mbh;
    for (int idx = 1; idx <= 50; ++idx) {
      int size = randomGenerator.nextInt(1024 * 1024) + 1024 * 1024;
      mbh = mclst.createBuffer(size);
      if (null == mbh) {
        throw new OutOfMemoryError("Memory Cluster out of memory!");
      }
      for (int i = 0; i < size; i++) {
        /**
         * Manipulate the ByteBuffer backed by external memory resource. Note:
         * Do not assigned internal ByteBuffer object to any variable, only use
         * function get() to access it all time.
         */
        mbh.get().put((byte) randomGenerator.nextInt(255));
      }
      System.out.println(String.format("[Seq.%d] size %d - %d, (%s)", idx, size, mbh.get().capacity(),
          size == mbh.get().capacity() ? "Correct" : "Failed!!!"));
      if (idx % 6 == 0) {
        /**
         * Force to release memory resource of specified ByteBuffer object
         * immediately.
         */
        System.out.println(String.format("Manually destroy Buffer  at %X.", System.identityHashCode(mbh.get())));
        mbh.destroy();
      }
      System.out.println("-------------------");
    }
    // Utils.collectGarbage();

    /**
     * Start to create a piece of memory resource backed chunk and then
     * automatically release it or manually release it every six chunks.
     */
    System.out.println(Utils.ANSI_GREEN + "[[Demo Allocation, Auto Destruction "
        + "and Manual Destruction of Big Memory Chunk.]]" + Utils.ANSI_RESET);
    @SuppressWarnings({"restriction", "UseOfSunClasses"})
    sun.misc.Unsafe unsafe = Utils.getUnsafe();
    MemChunkHolder<?> mch;
    for (int idx = 1; idx <= 50; ++idx) {
      int size = randomGenerator.nextInt(1024 * 1024) + 1024 * 1024;
      mch = mclst.createChunk(size);
      if (null == mch) {
        throw new OutOfMemoryError("Memory Cluster out of memory!");
      }
      // mch.cancelAutoReclaim();
      mch.resize(size - 10);
      System.out.printf("chunk size is %d \n", mch.getSize());
      for (int i = 0; i < mch.getSize(); i++) {
        /**
         * Manipulate the chunk data. Note: Do not assigned the internal memory
         * space to any variable, please always use function get() to access it
         * at all time.
         */
        unsafe.putByte(mch.get() + i, (byte) randomGenerator.nextInt(255));
      }
      System.out.println(String.format("[Seq.%d] size %d - %d at %X.", idx, size, mch.getSize(), mch.get()));
      if (idx % 6 == 0) {
        /**
         * Force to release memory resource of specified Chunk object
         * Immediately.
         */
        System.out.println(String.format("Manually destroy Chunk at %X.", mch.get()));
        mch.destroy();
      }
      System.out.println("-------------------");
    }
  }

}
