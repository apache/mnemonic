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

import org.apache.mnemonic.service.allocatorservice.VolatileMemoryAllocatorService;
import org.flowcomputing.commons.resgc.ResCollector;
import org.flowcomputing.commons.resgc.ResReclaim;

/**
 * manage a big native memory pool through libvmem.so that is provied by Intel
 * nvml library.
 * 
 *
 */
public class VolatileMemAllocator extends CommonAllocator<VolatileMemAllocator> {

  private boolean m_activegc = true;
  private long m_gctimeout = 100;
  private long m_nid = -1;
  private VolatileMemoryAllocatorService m_vmasvc = null;

  /**
   * Constructor, it initializes and allocate a memory pool from specified uri
   * location with specified capacity and an allocator service instance.
   * usually, the uri points to a mounted memory device or a location of file
   * system.
   * 
   * @param vmasvc
   *          the volatile memory allocation service instance
   *
   * @param capacity
   *          the capacity of memory pool
   * 
   * @param uri
   *          the location of memory pool will be created
   * 
   * @param isnew
   *          a place holder, always specify it as true
   */
  public VolatileMemAllocator(VolatileMemoryAllocatorService vmasvc, long capacity, String uri, boolean isnew) {
    assert null != vmasvc : "VolatileMemoryAllocatorService object is null";
    if (capacity <= 0) {
      throw new IllegalArgumentException("BigDataMemAllocator cannot be initialized with capacity <= 0.");
    }

    m_vmasvc = vmasvc;
    m_nid = m_vmasvc.init(capacity, uri, isnew);

    /**
     * create a resource collector to release specified bytebuffer that backed
     * by underlying big memory pool.
     */
    m_bufcollector = new ResCollector<MemBufferHolder<VolatileMemAllocator>, ByteBuffer>(new ResReclaim<ByteBuffer>() {
      @Override
      public void reclaim(ByteBuffer mres) {
        boolean cb_reclaimed = false;
        if (null != m_bufferreclaimer) {
          cb_reclaimed = m_bufferreclaimer.reclaim(mres, Long.valueOf(mres.capacity()));
        }
        if (!cb_reclaimed) {
          m_vmasvc.destroyByteBuffer(m_nid, mres);
          mres = null;
        }
      }
    });

    /**
     * create a resource collector to release specified chunk that backed by
     * underlying big memory pool.
     */
    m_chunkcollector = new ResCollector<MemChunkHolder<VolatileMemAllocator>, Long>(new ResReclaim<Long>() {
      @Override
      public void reclaim(Long mres) {
        // System.out.println(String.format("Reclaim: %X", mres));
        boolean cb_reclaimed = false;
        if (null != m_chunkreclaimer) {
          cb_reclaimed = m_chunkreclaimer.reclaim(mres, null);
        }
        if (!cb_reclaimed) {
          m_vmasvc.free(m_nid, mres);
          mres = null;
        }
      }
    });
  }

  /**
   * enable active garbage collection. the GC will be forced to collect garbages
   * when there is no more space for current allocation request.
   *
   * @param timeout
   *          the timeout is used to yield for GC performing
   *
   * @return this allocator
   */
  @Override
  public VolatileMemAllocator enableActiveGC(long timeout) {
    m_activegc = true;
    m_gctimeout = timeout;
    return this;
  }

  /**
   * disable active garbage collection.
   *
   * @return this allocator
   */
  @Override
  public VolatileMemAllocator disableActiveGC() {
    m_activegc = false;
    return this;
  }

  /**
   * release the memory pool and close it.
   *
   */
  @Override
  public void close() {
    super.close();
  }

  /**
   * force to synchronize uncommitted data to backed memory pool (this is a
   * placeholder).
   *
   */
  @Override
  public void sync() {
  }

  /**
   * re-size a specified chunk on its backed memory pool.
   * 
   * @param mholder
   *          the holder of memory chunk. it can be null.
   * 
   * @param size
   *          specify a new size of memory chunk
   * 
   * @return the resized memory chunk handler
   */
  @Override
  public MemChunkHolder<VolatileMemAllocator> resizeChunk(MemChunkHolder<VolatileMemAllocator> mholder, long size) {
    MemChunkHolder<VolatileMemAllocator> ret = null;
    boolean ac = null != mholder.getRefId();
    if (size > 0) {
      Long addr = m_vmasvc.reallocate(m_nid, mholder.get(), size, true);
      if (0 == addr && m_activegc) {
        m_chunkcollector.waitReclaimCoolDown(m_gctimeout);
        addr = m_vmasvc.reallocate(m_nid, mholder.get(), size, true);
      }
      if (0 != addr) {
        mholder.clear();
        mholder.destroy();
        ret = new MemChunkHolder<VolatileMemAllocator>(this, addr, size);
        if (ac) {
          m_chunkcollector.register(ret);
        }
      }
    }
    return ret;
  }

  /**
   * resize a specified buffer on its backed memory pool.
   *
   * @param mholder
   *          the holder of memory buffer. it can be null.
   * 
   * @param size
   *          specify a new size of memory chunk
   * 
   * @return the resized memory buffer handler
   *
   */
  @Override
  public MemBufferHolder<VolatileMemAllocator> resizeBuffer(MemBufferHolder<VolatileMemAllocator> mholder, long size) {
    MemBufferHolder<VolatileMemAllocator> ret = null;
    boolean ac = null != mholder.getRefId();
    if (size > 0) {
      int bufpos = mholder.get().position();
      int buflimit = mholder.get().limit();
      ByteBuffer buf = m_vmasvc.resizeByteBuffer(m_nid, mholder.get(), size);
      if (null == buf && m_activegc) {
        m_bufcollector.waitReclaimCoolDown(m_gctimeout);
        buf = m_vmasvc.resizeByteBuffer(m_nid, mholder.get(), size);
      }
      if (null != buf) {
        mholder.clear();
        mholder.destroy();
        buf.position(bufpos <= size ? bufpos : 0);
        buf.limit(buflimit <= size ? buflimit : (int) size);
        ret = new MemBufferHolder<VolatileMemAllocator>(this, buf);
        if (ac) {
          m_bufcollector.register(ret);
        }
      }
    }
    return ret;
  }

  /**
   * create a memory chunk that is managed by its holder.
   * 
   * @param size
   *          specify the size of memory chunk
   * 
   * @param autoreclaim
   *          specify whether or not to reclaim this chunk automatically
   *
   * @return a holder contains a memory chunk
   */
  @Override
  public MemChunkHolder<VolatileMemAllocator> createChunk(long size, boolean autoreclaim) {
    MemChunkHolder<VolatileMemAllocator> ret = null;
    Long addr = m_vmasvc.allocate(m_nid, size, true);
    if (0 == addr && m_activegc) {
      m_chunkcollector.waitReclaimCoolDown(m_gctimeout);
      addr = m_vmasvc.allocate(m_nid, size, true);
    }
    if (0 != addr) {
      ret = new MemChunkHolder<VolatileMemAllocator>(this, addr, size);
      ret.setCollector(m_chunkcollector);
      if (autoreclaim) {
        m_chunkcollector.register(ret);
      }
    }
    return ret;
  }

  /**
   * create a memory buffer that is managed by its holder.
   * 
   * @param size
   *          specify the size of memory buffer
   * 
   * @param autoreclaim
   *          specify whether or not to reclaim this buffer automatically
   *
   * @return a holder contains a memory buffer
   */
  @Override
  public MemBufferHolder<VolatileMemAllocator> createBuffer(long size, boolean autoreclaim) {
    MemBufferHolder<VolatileMemAllocator> ret = null;
    ByteBuffer bb = m_vmasvc.createByteBuffer(m_nid, size);
    if (null == bb && m_activegc) {
      m_bufcollector.waitReclaimCoolDown(m_gctimeout);
      bb = m_vmasvc.createByteBuffer(m_nid, size);
    }
    if (null != bb) {
      ret = new MemBufferHolder<VolatileMemAllocator>(this, bb);
      ret.setCollector(m_bufcollector);
      if (autoreclaim) {
        m_bufcollector.register(ret);
      }
    }
    return ret;
  }

}
