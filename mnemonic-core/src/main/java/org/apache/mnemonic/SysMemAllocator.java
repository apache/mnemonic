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

import org.apache.mnemonic.resgc.ContextWrapper;
import org.apache.mnemonic.resgc.ResCollector;
import org.apache.mnemonic.resgc.ResReclaim;
import org.apache.mnemonic.resgc.ReclaimContext;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.mnemonic.service.memory.MemoryServiceFeature.EXPANDABLE;
import static org.apache.mnemonic.service.memory.MemoryServiceFeature.SHRINKABLE;

/**
 * manage a system memory pool as a internal volatile allocator
 *
 */
@SuppressWarnings("restriction")
public class SysMemAllocator extends CommonAllocator<SysMemAllocator> {

  private boolean m_activegc = true;
  private long m_gctimeout = 100;
  @SuppressWarnings({"restriction", "UseOfSunClasses"})
  private static sun.misc.Unsafe m_unsafe = null;
  private AtomicLong currentMemory = new AtomicLong(0L);
  private long maxStoreCapacity = 0L;
  private Map<Long, Long> m_chunksize = new ConcurrentHashMap<Long, Long>();

  private static Method buf_clean_method;

  /**
   * Constructor, it initialize and allocate a memory pool from Java off-heap
   * with specified capacity.
   *
   * @param capacity
   *          specify the capacity of a system memory pool
   *
   * @param isnew
   *          a place holder, always specify it as true
   *
   * @throws Exception
   *           fail to retrieve Unsafe object
   *
   */
  public SysMemAllocator(long capacity, boolean isnew) throws Exception {
    m_unsafe = Utils.getUnsafe();
    buf_clean_method = sun.misc.Unsafe.class.getMethod("invokeCleaner", java.nio.ByteBuffer.class);
    maxStoreCapacity = capacity;
    /**
     * create a resource collector to release specified bytebuffer that backed
     * by Java off-heap.
     */
    m_bufcollector = new ResCollector<MemBufferHolder<SysMemAllocator>, ByteBuffer>(new ResReclaim<ByteBuffer>() {
      @Override
      public synchronized void reclaim(ContextWrapper<ByteBuffer> rctx) {
        ByteBuffer mres = rctx.getRes();
        Long sz = Long.valueOf(mres.capacity());
        boolean cb_reclaimed = false;
        if (null != m_bufferreclaimer) {
          cb_reclaimed = m_bufferreclaimer.reclaim(mres, sz);
        }
        if (!cb_reclaimed) {
          try {
            buf_clean_method.invoke(m_unsafe, mres);
          } catch (Exception e) {
            throw new Error(e);
          }
          mres = null;
        }
        currentMemory.addAndGet(-sz);
      }
    });

    /**
     * create a resource collector to release specified chunk that backed by
     * Java off-heap.
     */
    m_chunkcollector = new ResCollector<MemChunkHolder<SysMemAllocator>, Long>(new ResReclaim<Long>() {
      @Override
      public synchronized void reclaim(ContextWrapper<Long> rctx) {
        Long mres = rctx.getRes();
        // System.out.println(String.format("Reclaim: %X ...", mres));
        Long sz = m_chunksize.remove(mres);
        boolean cb_reclaimed = false;
        if (null != m_chunkreclaimer) {
          cb_reclaimed = m_chunkreclaimer.reclaim(mres, sz);
        }
        if (!cb_reclaimed) {
          m_unsafe.freeMemory(mres);
          mres = null;
        }
        if (null != sz) {
          currentMemory.addAndGet(-sz);
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
  public SysMemAllocator enableActiveGC(long timeout) {
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
  public SysMemAllocator disableActiveGC() {
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
   * sync. dirty data to memory
   *
   * @param addr
   *          specify the address
   *
   * @param length
   *          specify the length
   *
   * @param autodetect
   *          detect the length of specified memory block
   *
   */
  @Override
  public void syncToVolatileMemory(long addr, long length, boolean autodetect) {
    throw new UnsupportedOperationException("SysMemAllocator doesn't support sync");
  }

  /**
   * sync. a buffer to memory.
   *
   * @param mbuf specify a buffer to be sync.
   */
  @Override
  public void syncToVolatileMemory(MemBufferHolder<SysMemAllocator> mbuf) {
    throw new UnsupportedOperationException("SysMemAllocator doesn't support sync");
  }

  /**
   * sync. a chunk to memory.
   *
   * @param mchunk specify a chunk to be sync.
   */
  @Override
  public void syncToVolatileMemory(MemChunkHolder<SysMemAllocator> mchunk) {
    throw new UnsupportedOperationException("SysMemAllocator doesn't support sync");
  }

  @Override
  public void syncAll() {
    throw new UnsupportedOperationException("SysMemAllocator doesn't support sync");
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
  public MemChunkHolder<SysMemAllocator> resizeChunk(MemChunkHolder<SysMemAllocator> mholder, long size) {
    MemChunkHolder<SysMemAllocator> ret = null;
    boolean ac = null != mholder.getRefId();
    if (size > 0) {
      if (currentMemory.get() + size > maxStoreCapacity) {
        if (m_activegc) {
          m_chunkcollector.waitReclaimCoolDown(m_gctimeout);
        }
      }
      if (currentMemory.get() + size <= maxStoreCapacity) {
        Long addr = m_unsafe.reallocateMemory(mholder.get(), size);
        if (0 != addr) {
          mholder.clear();
          mholder.destroy();
          ret = new MemChunkHolder<SysMemAllocator>(this, addr, size);
          if (ac) {
            m_chunkcollector.register(ret);
          }
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
  public MemBufferHolder<SysMemAllocator> resizeBuffer(MemBufferHolder<SysMemAllocator> mholder, long size) {
    MemBufferHolder<SysMemAllocator> ret = null;
    boolean ac = null != mholder.getRefId();
    if (size > 0) {
      int bufpos = mholder.get().position();
      int buflimit = mholder.get().limit();
      if (currentMemory.get() + size > maxStoreCapacity) {
        if (m_activegc) {
          m_bufcollector.waitReclaimCoolDown(m_gctimeout);
        }
      }
      if (currentMemory.get() + size <= maxStoreCapacity) {
        ByteBuffer buf = Utils.resizeByteBuffer(mholder.get(), size);
        if (null != buf) {
          mholder.clear();
          mholder.destroy();
          buf.position(bufpos <= size ? bufpos : 0);
          buf.limit(buflimit <= size ? buflimit : (int) size);
          ret = new MemBufferHolder<SysMemAllocator>(this, buf);
          if (ac) {
            m_bufcollector.register(ret);
          }
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
   * @param rctx
   *          specify a reclaim context
   *
   * @return a holder contains a memory chunk
   */
  @Override
  public MemChunkHolder<SysMemAllocator> createChunk(long size, boolean autoreclaim,
                                                     ReclaimContext rctx) {
    MemChunkHolder<SysMemAllocator> ret = null;
    Long addr = null;
    if (currentMemory.get() + size > maxStoreCapacity) {
      if (m_activegc) {
        m_chunkcollector.waitReclaimCoolDown(m_gctimeout);
      }
    }
    if (currentMemory.get() + size <= maxStoreCapacity) {
      addr = m_unsafe.allocateMemory(size);
    }
    if (null != addr && 0 != addr) {
      ret = new MemChunkHolder<SysMemAllocator>(this, addr, size);
      ret.setCollector(m_chunkcollector);
      if (autoreclaim) {
        m_chunkcollector.register(ret, rctx);
      }
      m_chunksize.put(addr, size);
      currentMemory.getAndAdd(size);
    }
    return ret;
  }

  /**
   * create a memory buffer that is managed by its holder.
   *
   * @param size
   *          specify the size of memory buffer
   *
   * @param rctx
   *          specify a reclaim context
   *
   * @return a holder contains a memory buffer
   */
  @Override
  public MemBufferHolder<SysMemAllocator> createBuffer(long size, boolean autoreclaim,
                                                       ReclaimContext rctx) {
    MemBufferHolder<SysMemAllocator> ret = null;
    ByteBuffer bb = null;
    if (currentMemory.get() + size > maxStoreCapacity) {
      if (m_activegc) {
        m_bufcollector.waitReclaimCoolDown(m_gctimeout);
      }
    }
    if (currentMemory.get() + size <= maxStoreCapacity) {
      bb = ByteBuffer.allocateDirect((int) size);
    }
    if (null != bb) {
      ret = new MemBufferHolder<SysMemAllocator>(this, bb);
      ret.setCollector(m_bufcollector);
      if (autoreclaim) {
        m_bufcollector.register(ret, rctx);
      }
      currentMemory.getAndAdd(size);
    }
    return ret;
  }

  @Override
  public long expand(long size) {
    long ret = 0L;
    if (null != m_features) {
      if (m_features.contains(EXPANDABLE)) {
        ret = size;
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
      if (m_features.contains(SHRINKABLE)) {
        ret = size;
      } else {
        throw new ConfigurationException("Do not support shrink operation");
      }
    } else {
      throw new ConfigurationException("Do not support features");
    }
    return ret;
  }
}
