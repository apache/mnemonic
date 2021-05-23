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

import org.apache.mnemonic.query.memory.EntityInfo;
import org.apache.mnemonic.query.memory.Queryable;
import org.apache.mnemonic.query.memory.ResultSet;
import org.apache.mnemonic.service.computing.ValueInfo;
import org.apache.mnemonic.service.memory.MemoryServiceFeature;
import org.apache.mnemonic.service.memory.NonVolatileMemoryAllocatorService;
import org.apache.mnemonic.resgc.ContextWrapper;
import org.apache.mnemonic.resgc.ResCollector;
import org.apache.mnemonic.resgc.ResReclaim;
import org.apache.mnemonic.resgc.ReclaimContext;

import java.nio.ByteBuffer;

/**
 * manage a big native persistent memory pool through underlying memory service.
 * 
 *
 */
public class NonVolatileMemAllocator extends RestorableAllocator<NonVolatileMemAllocator>
    implements AddressTranslator, Persistence<NonVolatileMemAllocator> {

  private boolean m_activegc = true;
  private long m_gctimeout = 100;
  private long m_nid = -1;
  private long[][] m_ttable;
  private NonVolatileMemoryAllocatorService m_nvmasvc = null;
  private Queryable m_querable = null;

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
   *          force to create new memory pool if true
   *          otherwise, load specified memory pool if exists, create new one if not exits
   */
  public NonVolatileMemAllocator(NonVolatileMemoryAllocatorService nvmasvc, long capacity, String uri, boolean isnew) {
    if (null == nvmasvc) {
      throw new IllegalArgumentException("NonVolatileMemoryAllocatorService object is null");
    }
    m_features = nvmasvc.getFeatures();
    if (!m_features.contains(MemoryServiceFeature.NONVOLATILE)) {
      throw new ConfigurationException("The specified memory service does not support non-volatile feature");
    }
    m_absaddr = m_features.contains(MemoryServiceFeature.ABSTRACTADDRESSING);
    if (isnew && capacity <= 0) {
      throw new IllegalArgumentException("NonVolatileMemAllocator cannot be initialized with capacity <= 0.");
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
      public void reclaim(ContextWrapper<Long> cw) {
        Long mres = cw.getRes();
        // System.out.println(String.format("Reclaim: %X", mres));
        boolean cb_reclaimed = false;
        if (null != m_chunkreclaimer) {
          cb_reclaimed = m_chunkreclaimer.reclaim(mres, null);
        }
        if (!cb_reclaimed) {
          m_nvmasvc.free(m_nid, mres, cw.getContext());
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
          public void reclaim(ContextWrapper<ByteBuffer> cw) {
            ByteBuffer mres = cw.getRes();
            boolean cb_reclaimed = false;
            if (null != m_bufferreclaimer) {
              cb_reclaimed = m_bufferreclaimer.reclaim(mres, Long.valueOf(mres.capacity()));
            }
            if (!cb_reclaimed) {
              m_nvmasvc.destroyByteBuffer(m_nid, mres, cw.getContext());
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
   * force to synchronize uncommitted data to memory.
   *
   */
  @Override
  public void syncToVolatileMemory(long addr, long length, boolean autodetect) {
    m_nvmasvc.syncToVolatileMemory(m_nid, addr, length, autodetect);
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
   * @param rctx
   *          specify a reclaim context
   *
   * @return a durable chunk contains a memory chunk
   */
  @Override
  public DurableChunk<NonVolatileMemAllocator> createChunk(long size, boolean autoreclaim,
                                                           ReclaimContext rctx) {
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
        m_chunkcollector.register(ret, rctx);
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
   * @param rctx
   *          specify a reclaim context
   *
   * @return a durable buffer contains a memory buffer
   */
  @Override
  public DurableBuffer<NonVolatileMemAllocator> createBuffer(long size, boolean autoreclaim,
                                                             ReclaimContext rctx) {
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
        m_bufcollector.register(ret, rctx);
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
   * @param rctx
   *          specify a reclaim context
   *
   * @return a durable buffer contains the retrieved memory buffer
   */
  @Override
  public DurableBuffer<NonVolatileMemAllocator> retrieveBuffer(long phandler, boolean autoreclaim,
                                                               ReclaimContext rctx) {
    DurableBuffer<NonVolatileMemAllocator> ret = null;
    ByteBuffer bb = m_nvmasvc.retrieveByteBuffer(m_nid, getEffectiveAddress(phandler));
    if (null != bb) {
      ret = new DurableBuffer<NonVolatileMemAllocator>(this, bb);
      ret.setCollector(m_bufcollector);
      if (autoreclaim) {
        m_bufcollector.register(ret, rctx);
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
   * @param rctx
   *          specify a reclaim context
   *
   * @return a durable chunk contains the retrieved memory chunk
   */
  @Override
  public DurableChunk<NonVolatileMemAllocator> retrieveChunk(long phandler, boolean autoreclaim,
                                                             ReclaimContext rctx) {
    DurableChunk<NonVolatileMemAllocator> ret = null;
    long eaddr = getEffectiveAddress(phandler);
    long sz = m_nvmasvc.retrieveSize(m_nid, eaddr);
    if (sz > 0L) {
      ret = new DurableChunk<NonVolatileMemAllocator>(this, eaddr, sz);
      ret.setCollector(m_chunkcollector);
      if (autoreclaim) {
        m_chunkcollector.register(ret, rctx);
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
  @Override
  public void syncToVolatileMemory(MemBufferHolder<NonVolatileMemAllocator> mbuf) {
    m_nvmasvc.syncToVolatileMemory(m_nid, getBufferAddress(mbuf), 0L, true);
  }

  /**
   * sync. a chunk to underlying memory device.
   *
   * @param mchunk
   *         specify a chunk to be sync.
   */
  @Override
  public void syncToVolatileMemory(MemChunkHolder<NonVolatileMemAllocator> mchunk) {
    m_nvmasvc.syncToVolatileMemory(m_nid, getChunkAddress(mchunk), 0L, true);
  }

  /**
   * sync. the memory pool to underlying memory device.
   */
  @Override
  public void syncAll() {
    m_nvmasvc.syncToVolatileMemory(m_nid, 0L, 0L, true);
  }

  /**
   * Make any cached changes to a memory resource persistent.
   *
   * @param addr       the address of a memory resource
   * @param length     the length of the memory resource
   * @param autodetect if NULL == address and autodetect : persist whole pool
   */
  @Override
  public void syncToNonVolatileMemory(long addr, long length, boolean autodetect) {
    m_nvmasvc.syncToNonVolatileMemory(m_nid, addr, length, autodetect);
  }

  /**
   * flush processors cache for a memory resource
   *
   * @param addr       the address of a memory resource
   * @param length     the length of the memory resource
   * @param autodetect if NULL == address and autodetect : flush whole pool
   */
  @Override
  public void syncToLocal(long addr, long length, boolean autodetect) {
    m_nvmasvc.syncToLocal(m_nid, addr, length, autodetect);
  }

  /**
   * persist a buffer to persistent memory.
   *
   * @param mbuf
   *         specify a buffer to be persisted
   */
  @Override
  public void syncToNonVolatileMemory(MemBufferHolder<NonVolatileMemAllocator> mbuf) {
    m_nvmasvc.syncToNonVolatileMemory(m_nid, getBufferAddress(mbuf), 0L, true);
  }

  /**
   * persist a chunk to persistent memory.
   *
   * @param mchunk
   *         specify a chunk to be persisted
   */
  @Override
  public void syncToNonVolatileMemory(MemChunkHolder<NonVolatileMemAllocator> mchunk) {
    m_nvmasvc.syncToNonVolatileMemory(m_nid, getChunkAddress(mchunk), 0L, true);
  }

  /**
   * persist the memory pool to persistent memory.
   */
  @Override
  public void persistAll() {
    m_nvmasvc.syncToNonVolatileMemory(m_nid, 0L, 0L, true);
  }

  /**
   * flush a buffer to persistent memory.
   * 
   * @param mbuf
   *         specify a buffer to be flushed
   */
  @Override
  public void syncToLocal(MemBufferHolder<NonVolatileMemAllocator> mbuf) {
    m_nvmasvc.syncToLocal(m_nid, getBufferAddress(mbuf), 0L, true);
  }

  /**
   * flush a chunk to persistent memory.
   * 
   * @param mchunk
   *         specify a chunk to be flushed
   */
  @Override
  public void syncToLocal(MemChunkHolder<NonVolatileMemAllocator> mchunk) {
    m_nvmasvc.syncToLocal(m_nid, getChunkAddress(mchunk), 0L, true);
  }

  /**
   * flush the memory pool to persistent memory.
   */
  @Override
  public void flushAll() {
    m_nvmasvc.syncToLocal(m_nid, 0L, 0L, true);
  }

  /**
   * drain memory caches to persistent memory.
   */
  @Override
  public void drain() {
    m_nvmasvc.drain(m_nid);
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
    if (useAbstractAddressing()) {
      return m_nvmasvc.getPortableAddress(addr);
    }
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
    if (useAbstractAddressing()) {
      return m_nvmasvc.getEffectiveAddress(addr);
    }
    int i;
    for (i = 0; i < m_ttable.length; ++i) {
      if (addr >= m_ttable[i][0] && addr < m_ttable[i][1]) {
        return addr + m_ttable[i][2];
      }
    }
    throw new AddressTranslateError("Effective Address Translate Error");
  }

  @Override
  public byte[] getAbstractAddress(long addr) {
    byte[] ret;
    if (useAbstractAddressing()) {
      ret = m_nvmasvc.getAbstractAddress(addr);
    } else {
      throw new ConfigurationException("Do not support get abstract address operation");
    }
    return ret;
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

  /**
   * start a transaction
   *
   * @param readOnly
   *          specify if the transaction is readonly
   */
  @Override
  public void begin(boolean readOnly) {
    m_nvmasvc.beginTransaction(readOnly);
  }

  /**
   * commit current transaction.
   */
  @Override
  public void commit() {
    m_nvmasvc.commitTransaction();
  }

  /**
   * abort current transaction
   */
  @Override
  public void abort() {
    m_nvmasvc.abortTransaction();
  }

  /**
   * determine if in a transaction
   *
   * @return the true if it is in a transaction
   */
  @Override
  public boolean isInTransaction() {
    return m_nvmasvc.isInTransaction();
  }

  /**
   * The class is used as an adapter object for memory service based query operations 
   */
  class MemoryQueryAdapter implements Queryable {

    @Override
    public String[] getClassNames() {
      return m_nvmasvc.getClassNames(m_nid);
    }

    @Override
    public String[] getEntityNames(String clsname) {
      return m_nvmasvc.getEntityNames(m_nid, clsname);
    }

    @Override
    public EntityInfo getEntityInfo(String clsname, String etyname) {
      return m_nvmasvc.getEntityInfo(m_nid, clsname, etyname);
    }

    @Override
    public void createEntity(EntityInfo entityinfo) {
      m_nvmasvc.createEntity(m_nid, entityinfo);
    }

    @Override
    public void destroyEntity(String clsname, String etyname) {
      m_nvmasvc.destroyEntity(m_nid, clsname, etyname);
    }

    @Override
    public void updateQueryableInfo(String clsname, String etyname, ValueInfo updobjs) {
      m_nvmasvc.updateQueryableInfo(m_nid, clsname, etyname, updobjs);
    }

    @Override
    public void deleteQueryableInfo(String clsname, String etyname, ValueInfo updobjs) {
      m_nvmasvc.deleteQueryableInfo(m_nid, clsname, etyname, updobjs);
    }

    @Override
    public ResultSet query(String querystr) {
      return m_nvmasvc.query(m_nid, querystr);
    }
  }

  /**
   * Get a queryable object
   *f
   * @return a queryable object
   */
  Queryable useQuery() {
    if (null == m_querable) {
      if (m_features.contains(MemoryServiceFeature.QUERYABLE)) {
        m_querable = new MemoryQueryAdapter();
      }
    }
    return m_querable;
  }

  @Override
  public long expand(long size) {
    long ret = 0L;
    if (null != m_features) {
      if (m_features.contains(MemoryServiceFeature.EXPANDABLE)) {
        ret = m_nvmasvc.adjustCapacity(m_nid, size);
      } else {
        throw new ConfigurationException("Do not support expand operation");
      }
    } else {
      throw new ConfigurationException("Do not support features");
    }
    return ret;
  }

  @Override
  public long shrink(long size) {
    long ret = 0L;
    if (null != m_features) {
      if (m_features.contains(MemoryServiceFeature.SHRINKABLE)) {
        ret = m_nvmasvc.adjustCapacity(m_nid, (-1) * size);
      } else {
        throw new ConfigurationException("Do not support shrink operation");
      }
    } else {
      throw new ConfigurationException("Do not support features");
    }
    return ret;
  }
}
