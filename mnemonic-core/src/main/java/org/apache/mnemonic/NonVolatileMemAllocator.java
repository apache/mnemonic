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
import org.apache.mnemonic.service.memoryservice.NonVolatileMemoryAllocatorService;
import org.flowcomputing.commons.resgc.ResCollector;
import org.flowcomputing.commons.resgc.ResReclaim;

/**
 * manage a big native persistent memory pool through libpmalloc.so provided by
 * pmalloc project.
 * 
 *
 */
public class NonVolatileMemAllocator extends RestorableAllocator<NonVolatileMemAllocator>
    implements AddressTranslator {

  private boolean m_activegc = true;
  private long m_gctimeout = 100;
  private long m_nid = -1;
  private long[][] m_ttable;
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
    m_ttable = new long[1][3];
    m_ttable[0][0] = 0L;
    m_ttable[0][1] = m_nvmasvc.capacity(m_nid);
    m_ttable[0][2] = m_nvmasvc.getBaseAddress(m_nid);

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
   * @return the resized durable memory chunk handler
   */
  @Override
  public DurableChunk<NonVolatileMemAllocator> resizeChunk(MemChunkHolder<NonVolatileMemAllocator> mholder,
      long size) {
    DurableChunk<NonVolatileMemAllocator> ret = null;
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
        ret = new DurableChunk<NonVolatileMemAllocator>(this, addr, size);
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
   * @return the resized durable memory buffer handler
   *
   */
  @Override
  public DurableBuffer<NonVolatileMemAllocator> resizeBuffer(MemBufferHolder<NonVolatileMemAllocator> mholder,
      long size) {
    DurableBuffer<NonVolatileMemAllocator> ret = null;
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
        ret = new DurableBuffer<NonVolatileMemAllocator>(this, buf);
        if (ac) {
          m_bufcollector.register(ret);
        }
      }
    }
    return ret;
  }

  /**
   * create a durable chunk that is managed by its holder.
   * 
   * @param size
   *          specify the size of memory chunk
   * 
   * @param autoreclaim
   *          specify whether or not to reclaim this chunk automatically
   *
   * @return a durable chunk contains a memory chunk
   */
  @Override
  public DurableChunk<NonVolatileMemAllocator> createChunk(long size, boolean autoreclaim) {
    DurableChunk<NonVolatileMemAllocator> ret = null;
    Long addr = m_nvmasvc.allocate(m_nid, size, true);
    if ((null == addr || 0 == addr) && m_activegc) {
      m_chunkcollector.waitReclaimCoolDown(m_gctimeout);
      addr = m_nvmasvc.allocate(m_nid, size, true);
    }
    if (null != addr && 0 != addr) {
      ret = new DurableChunk<NonVolatileMemAllocator>(this, addr, size);
      ret.setCollector(m_chunkcollector);
      if (autoreclaim) {
        m_chunkcollector.register(ret);
      }
    }
    return ret;
  }

  /**
   * create a durable buffer.
   * 
   * @param size
   *          specify the size of memory buffer
   * 
   * @param autoreclaim
   *          specify whether or not to reclaim this buffer automatically
   *
   * @return a durable buffer contains a memory buffer
   */
  @Override
  public DurableBuffer<NonVolatileMemAllocator> createBuffer(long size, boolean autoreclaim) {
    DurableBuffer<NonVolatileMemAllocator> ret = null;
    ByteBuffer bb = m_nvmasvc.createByteBuffer(m_nid, size);
    if (null == bb && m_activegc) {
      m_bufcollector.waitReclaimCoolDown(m_gctimeout);
      bb = m_nvmasvc.createByteBuffer(m_nid, size);
    }
    if (null != bb) {
      ret = new DurableBuffer<NonVolatileMemAllocator>(this, bb);
      ret.setCollector(m_bufcollector);
      if (autoreclaim) {
        m_bufcollector.register(ret);
      }
    }
    return ret;
  }

  /**
   * retrieve a durable buffer from its backed memory allocator.
   * 
   * @param phandler
   *          specify the handler of memory buffer to retrieve
   *
   * @param autoreclaim
   *          specify whether this retrieved memory buffer can be reclaimed
   *          automatically or not
   * 
   * @return a durable buffer contains the retrieved memory buffer
   */
  @Override
  public DurableBuffer<NonVolatileMemAllocator> retrieveBuffer(long phandler, boolean autoreclaim) {
    DurableBuffer<NonVolatileMemAllocator> ret = null;
    ByteBuffer bb = m_nvmasvc.retrieveByteBuffer(m_nid, getEffectiveAddress(phandler));
    if (null != bb) {
      ret = new DurableBuffer<NonVolatileMemAllocator>(this, bb);
      ret.setCollector(m_bufcollector);
      if (autoreclaim) {
        m_bufcollector.register(ret);
      }
    }
    return ret;
  }

  /**
   * retrieve a durable chunk from its backed memory allocator.
   * 
   * @param phandler
   *          specify the handler of memory chunk to retrieve
   *
   * @param autoreclaim
   *          specify whether this retrieved memory chunk can be reclaimed
   *          automatically or not
   * 
   * @return a durable chunk contains the retrieved memory chunk
   */
  @Override
  public DurableChunk<NonVolatileMemAllocator> retrieveChunk(long phandler, boolean autoreclaim) {
    DurableChunk<NonVolatileMemAllocator> ret = null;
    long eaddr = getEffectiveAddress(phandler);
    long sz = m_nvmasvc.retrieveSize(m_nid, eaddr);
    if (sz > 0L) {
      ret = new DurableChunk<NonVolatileMemAllocator>(this, eaddr, sz);
      ret.setCollector(m_chunkcollector);
      if (autoreclaim) {
        m_chunkcollector.register(ret);
      }
    }
    return ret;
  }

  /**
   * get the address from a memory buffer holder.
   * 
   * @param mbuf
   *          specify the memory buffer holder
   *
   * @return an address that could be used to retrieve its memory buffer
   */
  @Override
  public long getBufferAddress(MemBufferHolder<NonVolatileMemAllocator> mbuf) {
    return m_nvmasvc.getByteBufferHandler(m_nid, mbuf.get());
  }

  /**
   * get the address from a memory chunk holder.
   * 
   * @param mchunk
   *          specify the memory chunk holder
   *
   * @return an address that could be used to retrieve its memory chunk
   */
  @Override
  public long getChunkAddress(MemChunkHolder<NonVolatileMemAllocator> mchunk) {
    return mchunk.get();
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
   * sync. a buffer to underlying memory device.
   * 
   * @param mbuf
   *         specify a buffer to be sync.
   */
  public void sync(MemBufferHolder<NonVolatileMemAllocator> mbuf) {
    m_nvmasvc.sync(m_nid, getBufferAddress(mbuf), 0L, true);
  }

  /**
   * sync. a chunk to underlying memory device.
   * 
   * @param mchunk
   *         specify a chunk to be sync.
   */
  public void sync(MemChunkHolder<NonVolatileMemAllocator> mchunk) {
    m_nvmasvc.sync(m_nid, getChunkAddress(mchunk), 0L, true);
  }

  /**
   * sync. the memory pool to underlying memory device.
   */
  public void syncAll() {
    m_nvmasvc.sync(m_nid, 0L, 0L, true);
  }

  /**
   * persist a buffer to persistent memory.
   * 
   * @param mbuf
   *         specify a buffer to be persisted
   */
  public void persist(MemBufferHolder<NonVolatileMemAllocator> mbuf) {
    m_nvmasvc.persist(m_nid, getBufferAddress(mbuf), 0L, true);
  }

  /**
   * persist a chunk to persistent memory.
   * 
   * @param mchunk
   *         specify a chunk to be persisted
   */
  public void persist(MemChunkHolder<NonVolatileMemAllocator> mchunk) {
    m_nvmasvc.persist(m_nid, getChunkAddress(mchunk), 0L, true);
  }

  /**
   * persist the memory pool to persistent memory.
   */
  public void persistAll() {
    m_nvmasvc.persist(m_nid, 0L, 0L, true);
  }

  /**
   * flush a buffer to persistent memory.
   * 
   * @param mbuf
   *         specify a buffer to be flushed
   */
  public void flush(MemBufferHolder<NonVolatileMemAllocator> mbuf) {
    m_nvmasvc.flush(m_nid, getBufferAddress(mbuf), 0L, true);
  }

  /**
   * flush a chunk to persistent memory.
   * 
   * @param mchunk
   *         specify a chunk to be flushed
   */
  public void flush(MemChunkHolder<NonVolatileMemAllocator> mchunk) {
    m_nvmasvc.flush(m_nid, getChunkAddress(mchunk), 0L, true);
  }

  /**
   * flush the memory pool to persistent memory.
   */
  public void flushAll() {
    m_nvmasvc.flush(m_nid, 0L, 0L, true);
  }

  /**
   * drain memory caches to persistent memory.
   */
  public void drain() {
    m_nvmasvc.drain(m_nid);
  }

  /**
   * determine whether the allocator supports transaction feature or not
   *
   * @return true if supported
   */
  @Override
  public boolean supportTransaction() {
    return false;
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
   * determine whether the allocator does atomic operations on memory pool
   *
   * @return true if it does
   *
   */
  @Override
  public boolean isAtomicOperation() {
    return false;
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
  @Override
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
  @Override
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
   * translate the portable address
   *
   * @param addr
   *          the address to be translated
   *
   * @return the portable address
   */
  @Override
  public long getPortableAddress(long addr) {
    int i;
    for (i = 0; i < m_ttable.length; ++i) {
      if (addr >= m_ttable[i][2] && addr < m_ttable[i][1] + m_ttable[i][2]) {
        return addr - m_ttable[i][2];
      }
    }
    throw new AddressTranslateError("Portable Address Translate Error");
  }

  /**
   * translate the effective address
   *
   * @param addr
   *          the address to be translated
   *
   * @return the effective address
   */
  @Override
  public long getEffectiveAddress(long addr) {
    int i;
    for (i = 0; i < m_ttable.length; ++i) {
      if (addr >= m_ttable[i][0] && addr < m_ttable[i][1]) {
        return addr + m_ttable[i][2];
      }
    }
    throw new AddressTranslateError("Effective Address Translate Error");
  }

  /**
   * get the address translate table
   *
   * @return the translate table
   */
  @Override
  public long[][] getTranslateTable() {
    return m_ttable;
  }

  /**
   * set address translate table
   *
   * @param tbl
   *         specify a translate table
   */
  @Override
  public void setTranslateTable(long[][] tbl) {
    m_ttable = tbl;
  }

}
