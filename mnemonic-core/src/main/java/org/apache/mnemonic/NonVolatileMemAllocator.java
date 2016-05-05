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

import org.apache.mnemonic.service.allocatorservice.NonVolatileMemoryAllocatorService;
import org.flowcomputing.commons.resgc.ResCollector;
import org.flowcomputing.commons.resgc.ResReclaim;

/**
 * manage a big native persistent memory pool through libpmalloc.so provided by
 * pmalloc project.
 * 
 *
 */
public class NonVolatileMemAllocator extends CommonDurableAllocator<NonVolatileMemAllocator>
    implements NVMAddressTranslator {

  private boolean m_activegc = true;
  private long m_gctimeout = 100;
  private long m_nid = -1;
  private long b_addr = 0;
  private NonVolatileMemoryAllocatorService m_nvmasvc = null;

  /**
   * Constructor, it initializes and allocate a memory pool from specified uri
   * location with specified capacity and an allocator service instance.
   * usually, the uri points to a mounted non-volatile memory device or a
   * location of file system.
   * 
   * @param nvmasvc
   *          the non-volatile memory allocation service instance
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
  public NonVolatileMemAllocator(NonVolatileMemoryAllocatorService nvmasvc, long capacity, String uri, boolean isnew) {
    assert null != nvmasvc : "NonVolatileMemoryAllocatorService object is null";
    if (capacity <= 0) {
      throw new IllegalArgumentException("BigDataPMemAllocator cannot be initialized with capacity <= 0.");
    }

    m_nvmasvc = nvmasvc;

    m_nid = m_nvmasvc.init(capacity, uri, isnew);
    b_addr = m_nvmasvc.getBaseAddress(m_nid);

    /**
     * create a resource collector to release specified chunk that backed by
     * underlying big memory pool.
     */
    m_chunkcollector = new ResCollector<MemChunkHolder<NonVolatileMemAllocator>, Long>(new ResReclaim<Long>() {
      @Override
      public void reclaim(Long mres) {
        // System.out.println(String.format("Reclaim: %X", mres));
        boolean cb_reclaimed = false;
        if (null != m_chunkreclaimer) {
          cb_reclaimed = m_chunkreclaimer.reclaim(mres, null);
        }
        if (!cb_reclaimed) {
          m_nvmasvc.free(m_nid, mres);
          mres = null;
        }
      }
    });

    /**
     * create a resource collector to release specified bytebuffer that backed
     * by underlying big memory pool.
     */
    m_bufcollector = new ResCollector<MemBufferHolder<NonVolatileMemAllocator>, ByteBuffer>(
        new ResReclaim<ByteBuffer>() {
          @Override
          public void reclaim(ByteBuffer mres) {
            boolean cb_reclaimed = false;
            if (null != m_bufferreclaimer) {
              cb_reclaimed = m_bufferreclaimer.reclaim(mres, Long.valueOf(mres.capacity()));
            }
            if (!cb_reclaimed) {
              m_nvmasvc.destroyByteBuffer(m_nid, mres);
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
   */
  @Override
  public NonVolatileMemAllocator enableActiveGC(long timeout) {
    m_activegc = true;
    m_gctimeout = timeout;
    return this;
  }

  /**
   * disable active garbage collection.
   * 
   */
  @Override
  public NonVolatileMemAllocator disableActiveGC() {
    m_activegc = false;
    return this;
  }

  /**
   * Release the memory pool and close it.
   *
   */
  @Override
  public void close() {
    super.close();
    m_nvmasvc.close(m_nid);
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
  public MemChunkHolder<NonVolatileMemAllocator> resizeChunk(MemChunkHolder<NonVolatileMemAllocator> mholder,
      long size) {
    MemChunkHolder<NonVolatileMemAllocator> ret = null;
    boolean ac = null != mholder.getRefId();
    if (size > 0) {
      Long addr = m_nvmasvc.reallocate(m_nid, mholder.get(), size, true);
      if (0 == addr && m_activegc) {
        m_chunkcollector.waitReclaimCoolDown(m_gctimeout);
        addr = m_nvmasvc.reallocate(m_nid, mholder.get(), size, true);
      }
      if (0 != addr) {
        mholder.clear();
        mholder.destroy();
        ret = new MemChunkHolder<NonVolatileMemAllocator>(this, addr, size);
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
  public MemBufferHolder<NonVolatileMemAllocator> resizeBuffer(MemBufferHolder<NonVolatileMemAllocator> mholder,
      long size) {
    MemBufferHolder<NonVolatileMemAllocator> ret = null;
    boolean ac = null != mholder.getRefId();
    if (size > 0) {
      int bufpos = mholder.get().position();
      int buflimit = mholder.get().limit();
      ByteBuffer buf = m_nvmasvc.resizeByteBuffer(m_nid, mholder.get(), size);
      if (null == buf && m_activegc) {
        m_bufcollector.waitReclaimCoolDown(m_gctimeout);
        buf = m_nvmasvc.resizeByteBuffer(m_nid, mholder.get(), size);
      }
      if (null != buf) {
        mholder.clear();
        mholder.destroy();
        buf.position(bufpos <= size ? bufpos : 0);
        buf.limit(buflimit <= size ? buflimit : (int) size);
        ret = new MemBufferHolder<NonVolatileMemAllocator>(this, buf);
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
  public MemChunkHolder<NonVolatileMemAllocator> createChunk(long size, boolean autoreclaim) {
    MemChunkHolder<NonVolatileMemAllocator> ret = null;
    Long addr = m_nvmasvc.allocate(m_nid, size, true);
    if ((null == addr || 0 == addr) && m_activegc) {
      m_chunkcollector.waitReclaimCoolDown(m_gctimeout);
      addr = m_nvmasvc.allocate(m_nid, size, true);
    }
    if (null != addr && 0 != addr) {
      ret = new MemChunkHolder<NonVolatileMemAllocator>(this, addr, size);
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
  public MemBufferHolder<NonVolatileMemAllocator> createBuffer(long size, boolean autoreclaim) {
    MemBufferHolder<NonVolatileMemAllocator> ret = null;
    ByteBuffer bb = m_nvmasvc.createByteBuffer(m_nid, size);
    if (null == bb && m_activegc) {
      m_bufcollector.waitReclaimCoolDown(m_gctimeout);
      bb = m_nvmasvc.createByteBuffer(m_nid, size);
    }
    if (null != bb) {
      ret = new MemBufferHolder<NonVolatileMemAllocator>(this, bb);
      ret.setCollector(m_bufcollector);
      if (autoreclaim) {
        m_bufcollector.register(ret);
      }
    }
    return ret;
  }

  /**
   * retrieve a memory buffer from its backed memory allocator.
   * 
   * @param phandler
   *          specify the handler of memory buffer to retrieve
   *
   * @param autoreclaim
   *          specify whether this retrieved memory buffer can be reclaimed
   *          automatically or not
   * 
   * @return a holder contains the retrieved memory buffer
   */
  @Override
  public MemBufferHolder<NonVolatileMemAllocator> retrieveBuffer(long phandler, boolean autoreclaim) {
    MemBufferHolder<NonVolatileMemAllocator> ret = null;
    ByteBuffer bb = m_nvmasvc.retrieveByteBuffer(m_nid, getEffectiveAddress(phandler));
    if (null != bb) {
      ret = new MemBufferHolder<NonVolatileMemAllocator>(this, bb);
      if (autoreclaim) {
        m_bufcollector.register(ret);
      }
    }
    return ret;
  }

  /**
   * retrieve a memory chunk from its backed memory allocator.
   * 
   * @param phandler
   *          specify the handler of memory chunk to retrieve
   *
   * @param autoreclaim
   *          specify whether this retrieved memory chunk can be reclaimed
   *          automatically or not
   * 
   * @return a holder contains the retrieved memory chunk
   */
  @Override
  public MemChunkHolder<NonVolatileMemAllocator> retrieveChunk(long phandler, boolean autoreclaim) {
    MemChunkHolder<NonVolatileMemAllocator> ret = null;
    long eaddr = getEffectiveAddress(phandler);
    long sz = m_nvmasvc.retrieveSize(m_nid, eaddr);
    if (sz > 0L) {
      ret = new MemChunkHolder<NonVolatileMemAllocator>(this, eaddr, sz);
      if (autoreclaim) {
        m_chunkcollector.register(ret);
      }
    }
    return ret;
  }

  /**
   * get the handler from a memory buffer holder.
   * 
   * @param mbuf
   *          specify the memory buffer holder
   *
   * @return a handler that could be used to retrieve its memory buffer
   */
  @Override
  public long getBufferHandler(MemBufferHolder<NonVolatileMemAllocator> mbuf) {
    return getPortableAddress(m_nvmasvc.getByteBufferHandler(m_nid, mbuf.get()));
  }

  /**
   * get the handler from a memory chunk holder.
   * 
   * @param mchunk
   *          specify the memory chunk holder
   *
   * @return a handler that could be used to retrieve its memory chunk
   */
  @Override
  public long getChunkHandler(MemChunkHolder<NonVolatileMemAllocator> mchunk) {
    return getPortableAddress(mchunk.get());
  }

  /**
   * determine whether this allocator supports to store non-volatile handler or
   * not. (it is a placeholder)
   *
   * @return true if there is
   */
  @Override
  public boolean hasDurableHandlerStore() {
    return true;
  }

  /**
   * start a application level transaction on this allocator. (it is a place
   * holder)
   *
   */
  @Override
  public void beginTransaction() {
    throw new UnsupportedOperationException("Transaction Unsupported.");
  }

  /**
   * end a application level transaction on this allocator. (it is a place
   * holder)
   *
   */
  @Override
  public void endTransaction() {
    throw new UnsupportedOperationException("Transaction Unsupported.");
  }

  /**
   * set a handler on key.
   * 
   * @param key
   *          the key to set its value
   * 
   * @param handler
   *          the handler
   */
  public void setHandler(long key, long handler) {
    m_nvmasvc.setHandler(m_nid, key, handler);
  }

  /**
   * get a handler value.
   * 
   * @param key
   *          the key to set its value
   * 
   * @return the value of handler
   */
  public long getHandler(long key) {
    return m_nvmasvc.getHandler(m_nid, key);
  }

  /**
   * return the capacity of non-volatile handler store.
   * 
   * @return the capacity of handler store
   * 
   */
  public long handlerCapacity() {
    return m_nvmasvc.handlerCapacity(m_nid);
  }

  /**
   * calculate the portable address
   *
   * @param addr
   *          the address to be calculated
   *
   * @return the portable address
   */
  @Override
  public long getPortableAddress(long addr) {
    return addr - b_addr;
  }

  /**
   * calculate the effective address
   *
   * @param addr
   *          the address to be calculated
   *
   * @return the effective address
   */
  @Override
  public long getEffectiveAddress(long addr) {
    return addr + b_addr;
  }

  /**
   * get the base address
   *
   * @return the base address
   */
  @Override
  public long getBaseAddress() {
    return b_addr;
  }

  /**
   * set the base address for calculation
   *
   * @param addr
   *          the base address
   *
   */
  @Override
  public long setBaseAddress(long addr) {
    return b_addr = addr;
  }

}
