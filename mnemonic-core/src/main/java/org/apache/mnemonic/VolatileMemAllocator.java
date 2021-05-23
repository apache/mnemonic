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
import org.apache.mnemonic.service.memory.VolatileMemoryAllocatorService;
import org.apache.mnemonic.resgc.ContextWrapper;
import org.apache.mnemonic.resgc.ResCollector;
import org.apache.mnemonic.resgc.ResReclaim;
import org.apache.mnemonic.resgc.ReclaimContext;

import java.nio.ByteBuffer;

/**
 * manage a big native memory pool through underlying memory service.
 * 
 *
 */
public class VolatileMemAllocator extends RestorableAllocator<VolatileMemAllocator> {

  private boolean m_activegc = true;
  private long m_gctimeout = 100;
  private long m_nid = -1;
  private long[][] m_ttable;
  private VolatileMemoryAllocatorService m_vmasvc = null;
  private Queryable m_querable = null;

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
   */
  public VolatileMemAllocator(VolatileMemoryAllocatorService vmasvc, long capacity, String uri) {
    if (null == vmasvc) {
      throw new IllegalArgumentException("VolatileMemoryAllocatorService object is null");
    }
    m_features = vmasvc.getFeatures();
    if (!m_features.contains(MemoryServiceFeature.VOLATILE)) {
      throw new ConfigurationException("The specified memory service does not support volatile feature");
    }
    m_absaddr = m_features.contains(MemoryServiceFeature.ABSTRACTADDRESSING);
    if (capacity <= 0) {
      throw new IllegalArgumentException("VolatileMemAllocator cannot be initialized with capacity <= 0.");
    }

    m_vmasvc = vmasvc;
    m_nid = m_vmasvc.init(capacity, uri, true);

    /**
     * create a resource collector to release specified bytebuffer that backed
     * by underlying big memory pool.
     */
    m_bufcollector = new ResCollector<MemBufferHolder<VolatileMemAllocator>, ByteBuffer>(new ResReclaim<ByteBuffer>() {
      @Override
      public void reclaim(ContextWrapper<ByteBuffer> cw) {
        ByteBuffer mres = cw.getRes();
        boolean cb_reclaimed = false;
        if (null != m_bufferreclaimer) {
          cb_reclaimed = m_bufferreclaimer.reclaim(mres, Long.valueOf(mres.capacity()));
        }
        if (!cb_reclaimed) {
          m_vmasvc.destroyByteBuffer(m_nid, mres, cw.getContext());
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
      public void reclaim(ContextWrapper<Long> cw) {
        Long mres = cw.getRes();
        // System.out.println(String.format("Reclaim: %X", mres));
        boolean cb_reclaimed = false;
        if (null != m_chunkreclaimer) {
          cb_reclaimed = m_chunkreclaimer.reclaim(mres, null);
        }
        if (!cb_reclaimed) {
          m_vmasvc.free(m_nid, mres, cw.getContext());
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

  @Override
  public long expand(long size) {
    long ret = 0L;
    if (null != m_features) {
      if (m_features.contains(MemoryServiceFeature.EXPANDABLE)) {
        ret = m_vmasvc.adjustCapacity(m_nid, size);
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
        ret = m_vmasvc.adjustCapacity(m_nid, (-1) * size);
      } else {
        throw new ConfigurationException("Do not support shrink operation");
      }
    } else {
      throw new ConfigurationException("Do not support features");
    }
    return ret;
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
   * force to synchronize uncommitted data to memory.
   *
   */
  @Override
  public void syncToVolatileMemory(long addr, long length, boolean autodetect) {
    m_vmasvc.syncToVolatileMemory(m_nid, addr, length, autodetect);
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
  public DurableChunk<VolatileMemAllocator> resizeChunk(MemChunkHolder<VolatileMemAllocator> mholder, long size) {
    DurableChunk<VolatileMemAllocator> ret = null;
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
        ret = new DurableChunk<VolatileMemAllocator>(this, addr, size);
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
  public DurableBuffer<VolatileMemAllocator> resizeBuffer(MemBufferHolder<VolatileMemAllocator> mholder, long size) {
    DurableBuffer<VolatileMemAllocator> ret = null;
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
        ret = new DurableBuffer<VolatileMemAllocator>(this, buf);
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
  public DurableChunk<VolatileMemAllocator> createChunk(long size, boolean autoreclaim,
                                                        ReclaimContext rctx) {
    DurableChunk<VolatileMemAllocator> ret = null;
    Long addr = m_vmasvc.allocate(m_nid, size, true);
    if (0 == addr && m_activegc) {
      m_chunkcollector.waitReclaimCoolDown(m_gctimeout);
      addr = m_vmasvc.allocate(m_nid, size, true);
    }
    if (0 != addr) {
      ret = new DurableChunk<VolatileMemAllocator>(this, addr, size);
      ret.setCollector(m_chunkcollector);
      if (autoreclaim) {
        m_chunkcollector.register(ret, rctx);
      }
    }
    return ret;
  }

  /**
   * create a durable buffer that is managed by its holder.
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
  public DurableBuffer<VolatileMemAllocator> createBuffer(long size, boolean autoreclaim,
                                                          ReclaimContext rctx) {
    DurableBuffer<VolatileMemAllocator> ret = null;
    ByteBuffer bb = m_vmasvc.createByteBuffer(m_nid, size);
    if (null == bb && m_activegc) {
      m_bufcollector.waitReclaimCoolDown(m_gctimeout);
      bb = m_vmasvc.createByteBuffer(m_nid, size);
    }
    if (null != bb) {
      ret = new DurableBuffer<VolatileMemAllocator>(this, bb);
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
  public DurableBuffer<VolatileMemAllocator> retrieveBuffer(long phandler, boolean autoreclaim,
                                                            ReclaimContext rctx) {
    DurableBuffer<VolatileMemAllocator> ret = null;
    ByteBuffer bb = m_vmasvc.retrieveByteBuffer(m_nid, getEffectiveAddress(phandler));
    if (null != bb) {
      ret = new DurableBuffer<VolatileMemAllocator>(this, bb);
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
  public DurableChunk<VolatileMemAllocator> retrieveChunk(long phandler, boolean autoreclaim,
                                                          ReclaimContext rctx) {
    DurableChunk<VolatileMemAllocator> ret = null;
    long eaddr = getEffectiveAddress(phandler);
    long sz = m_vmasvc.retrieveSize(m_nid, eaddr);
    if (sz > 0L) {
      ret = new DurableChunk<VolatileMemAllocator>(this, eaddr, sz);
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
  public long getBufferAddress(MemBufferHolder<VolatileMemAllocator> mbuf) {
    return m_vmasvc.getByteBufferHandler(m_nid, mbuf.get());
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
  public long getChunkAddress(MemChunkHolder<VolatileMemAllocator> mchunk) {
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
   * sync. a buffer to memory.
   *
   * @param mbuf
   *         specify a buffer to be sync.
   */
  @Override
  public void syncToVolatileMemory(MemBufferHolder<VolatileMemAllocator> mbuf) {
    m_vmasvc.syncToVolatileMemory(m_nid, getBufferAddress(mbuf), 0L, true);
  }

  /**
   * sync. a chunk to memory.
   *
   * @param mchunk
   *         specify a chunk to be sync.
   */
  @Override
  public void syncToVolatileMemory(MemChunkHolder<VolatileMemAllocator> mchunk) {
    m_vmasvc.syncToVolatileMemory(m_nid, getChunkAddress(mchunk), 0L, true);
  }

  /**
   * sync. the memory pool to underlying memory device.
   */
  @Override
  public void syncAll() {
    m_vmasvc.syncToVolatileMemory(m_nid, 0L, 0L, true);
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
    m_vmasvc.setHandler(m_nid, key, handler);
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
    return m_vmasvc.getHandler(m_nid, key);
  }

  /**
   * return the capacity of non-volatile handler store.
   * 
   * @return the capacity of handler store
   * 
   */
  public long handlerCapacity() {
    return m_vmasvc.handlerCapacity(m_nid);
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
      return m_vmasvc.getPortableAddress(addr);
    }
    int i;
    if (null == m_ttable) {
      return addr;
    }
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
      return m_vmasvc.getEffectiveAddress(addr);
    }
    int i;
    if (null == m_ttable) {
      return addr;
    }
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
      ret = m_vmasvc.getAbstractAddress(addr);
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
   *
   */
  @Override
  public void begin(boolean readOnly) {
    m_vmasvc.beginTransaction(readOnly);
  }

  /**
   * commit current transaction.
   */
  @Override
  public void commit() {
    m_vmasvc.commitTransaction();
  }

  /**
   * abort current transaction
   */
  @Override
  public void abort() {
    m_vmasvc.abortTransaction();
  }

  /**
   * determine if in a transaction
   *
   * @return the true if it is in a transaction
   */
  @Override
  public boolean isInTransaction() {
    return m_vmasvc.isInTransaction();
  }

  /**
   * The class is used as an adapter object for memory service based query operations
   */
  class MemoryQueryAdapter implements Queryable {

    @Override
    public String[] getClassNames() {
      return m_vmasvc.getClassNames(m_nid);
    }

    @Override
    public String[] getEntityNames(String clsname) {
      return m_vmasvc.getEntityNames(m_nid, clsname);
    }

    @Override
    public EntityInfo getEntityInfo(String clsname, String etyname) {
      return m_vmasvc.getEntityInfo(m_nid, clsname, etyname);
    }

    @Override
    public void createEntity(EntityInfo entityinfo) {
      m_vmasvc.createEntity(m_nid, entityinfo);
    }

    @Override
    public void destroyEntity(String clsname, String etyname) {
      m_vmasvc.destroyEntity(m_nid, clsname, etyname);
    }

    @Override
    public void updateQueryableInfo(String clsname, String etyname, ValueInfo updobjs) {
      m_vmasvc.updateQueryableInfo(m_nid, clsname, etyname, updobjs);
    }

    @Override
    public void deleteQueryableInfo(String clsname, String etyname, ValueInfo updobjs) {
      m_vmasvc.deleteQueryableInfo(m_nid, clsname, etyname, updobjs);
    }

    @Override
    public ResultSet query(String querystr) {
      return m_vmasvc.query(m_nid, querystr);
    }
  }

  /**
   * Get a queryable object
   *
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
}
