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

import org.apache.commons.lang3.RandomUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Test the functionality of ChunkBuffer
 */
public class ChunkBufferNGTest {

  private long m_keyid = 22L;
  private volatile long m_checksum;
  private volatile long m_count;
  private int m_bufsize = 90 * 1024 * 1024;

  @Test
  public void testGenChunkBuffers() {
    Checksum cs = new CRC32();
    cs.reset();

    NonVolatileMemAllocator act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService("pmalloc"),
            1024 * 1024 * 1024L, "./pmchunkbuffertest.dat", true);
    act.setChunkReclaimer(new Reclaim<Long>() {
      @Override
      public boolean reclaim(Long mres, Long sz) {
        System.out.println(String.format("Reclaim Memory Chunk: %X  Size: %s", System.identityHashCode(mres),
                null == sz ? "NULL" : sz.toString()));
        return false;
      }
    });
    DurableChunk<NonVolatileMemAllocator> mch;
    mch = act.createChunk(1000 * 1024 * 1024L);
    Assert.assertNotNull(mch);
    act.setHandler(m_keyid, mch.getHandler());
    long bufcnt = mch.getSize() / m_bufsize;
    ChunkBuffer ckbuf;
    byte[] rdbytes;
    for (long idx = 0; idx < bufcnt; ++idx) {
//      System.err.println(String.format("---- bufcnt: %d, bufsize: %d, idx: %d", bufcnt, m_bufsize, idx));
      ckbuf = mch.getChunkBuffer(idx * m_bufsize, m_bufsize);
      Assert.assertNotNull(ckbuf);
      rdbytes = RandomUtils.nextBytes(m_bufsize);
      Assert.assertNotNull(rdbytes);
      ckbuf.get().clear();
      ckbuf.get().put(rdbytes);
      cs.update(rdbytes, 0, rdbytes.length);
    }
    m_checksum = cs.getValue();
    m_count = bufcnt;
    act.close();
  }

  @Test(dependsOnMethods = {"testGenChunkBuffers"})
  public void testCheckChunkBuffers() {
    Checksum cs = new CRC32();
    cs.reset();
    NonVolatileMemAllocator act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService("pmalloc"),
            1L, "./pmchunkbuffertest.dat", false);
    act.setChunkReclaimer(new Reclaim<Long>() {
      @Override
      public boolean reclaim(Long mres, Long sz) {
        System.out.println(String.format("Reclaim Memory Chunk: %X  Size: %s", System.identityHashCode(mres),
                null == sz ? "NULL" : sz.toString()));
        return false;
      }
    });
    DurableChunk<NonVolatileMemAllocator> mch;
    mch = act.retrieveChunk(act.getHandler(m_keyid));
    Assert.assertNotNull(mch);
    long bufcnt = mch.getSize() / m_bufsize;

    ChunkBuffer ckbuf;
    byte[] buf;
    for (long idx = 0; idx < bufcnt; ++idx) {
      ckbuf = mch.getChunkBuffer(idx * m_bufsize, m_bufsize);
      Assert.assertNotNull(ckbuf);
      buf = new byte[m_bufsize];
      ckbuf.get().clear();
      ckbuf.get().get(buf);
      cs.update(buf, 0, buf.length);
    }
    act.close();

    Assert.assertEquals(m_checksum, cs.getValue());
    Assert.assertEquals(m_count, bufcnt);
    System.out.println(String.format("The checksum of chunk buffers are %d, Total count is %d", m_checksum, m_count));
  }

}
