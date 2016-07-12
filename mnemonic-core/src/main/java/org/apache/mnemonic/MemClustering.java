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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * clustering different kind of memory-like media and combine them as a larger
 * memory pool for allocation. it will notify user when the underlying memory
 * storage has been switched or downgraded for the request of allocation
 * 
 */
public class MemClustering {

  /**
   * an interface of event for performance level change.
   *
   */
  public interface PerformanceLevelChange {

    /**
     * callback if performance level changed
     *
     * @param prevlvl
     *          the perf. level before change
     *
     * @param tgtlvl
     *          the perf. level after change
     *
     */
    void changed(PerformanceLevel prevlvl, PerformanceLevel tgtlvl);
  }

  /**
   * an interface of event for CommonAllocator change.
   *
   */
  public interface AllocatorChange {

    /**
     * callback if allocator changed
     *
     * @param lvl
     *          the perf. level after changed
     *
     * @param prevallocator
     *          the allocator before change
     *
     * @param tgtallocator
     *          the allocator after change
     */
    void changed(PerformanceLevel lvl, CommonAllocator<?> prevallocator, CommonAllocator<?> tgtallocator);
  }

  /**
   * an interface to assist the creation of memory holder.
   *
   * @param <H>
   *          the holder type of memory resource
   *
   * @param bma
   *          specify an allocator for this holder
   *
   * @param size
   *          specify the size of this memory resource
   * 
   * @return the holder created
   */
  private interface MemCreate<H extends MemHolder<? extends CommonAllocator<?>, ?, ?>> {
    H create(CommonAllocator<?> bma, long size);
  }

  private MemCreate<MemChunkHolder<?>> m_memchunkcreate = new MemCreate<MemChunkHolder<?>>() {
    @Override
    public MemChunkHolder<?> create(CommonAllocator<?> bma, long size) {
      return bma.createChunk(size);
    }
  };

  private MemCreate<MemBufferHolder<?>> m_membuffercreate = new MemCreate<MemBufferHolder<?>>() {
    @Override
    public MemBufferHolder<?> create(CommonAllocator<?> bma, long size) {
      return bma.createBuffer(size);
    }
  };

  /**
   * performance level categories.
   *
   */
  public enum PerformanceLevel {
    FASTEST, FAST, NORMAL, SLOW, SLOWEST
  }

  /**
   * configuration for each allocator node.
   *
   */
  public static class NodeConfig<A extends CommonAllocator<A>> {
    private A m_allocator;
    private PerformanceLevel m_level;

    /**
     * Constructor: initialize this instance with specified allocator and perf.
     * level.
     * 
     * @param a
     *          specify an allocator for this node
     * 
     * @param l
     *          specify a performance level
     */
    public NodeConfig(A a, PerformanceLevel l) {
      m_allocator = a;
      m_level = l;
    }

    /**
     * retrieve the allocator of this node.
     * 
     * @return allocator of this node
     */
    public A getAllocator() {
      return m_allocator;
    }

    /**
     * retrieve the performance level of this node.
     * 
     * @return level of this node
     */
    public PerformanceLevel getPerformanceLevel() {
      return m_level;
    }
  }

  private PerformanceLevelChange m_bwlvlchange = null;
  private AllocatorChange m_allocatorChange = null;
  private PerformanceLevel m_prevbwlevel = null;
  private CommonAllocator<?> m_prevallocator = null;

  private Map<PerformanceLevel, List<CommonAllocator<?>>> m_info;

  /**
   * Constructor: initialize a memory clustering instance.
   * 
   * @param ncs
   *          specify a set of node with specified configuration respectively
   */
  public MemClustering(NodeConfig<? extends CommonAllocator<?>>[] ncs) {
    m_info = new EnumMap<PerformanceLevel, List<CommonAllocator<?>>>(PerformanceLevel.class);
    for (PerformanceLevel lvl : PerformanceLevel.values()) {
      m_info.put(lvl, new ArrayList<CommonAllocator<?>>());
    }

    for (NodeConfig<? extends CommonAllocator<?>> nc : ncs) {
      m_info.get(nc.getPerformanceLevel()).add(nc.getAllocator());
    }

  }

  /**
   * set a callback of event for performance level change.
   * 
   * @param bwlvlchange
   *          specify a callback object for perf. level change
   */
  public void setPerformanceLevelChange(PerformanceLevelChange bwlvlchange) {
    m_bwlvlchange = bwlvlchange;
  }

  /**
   * set a callback of event for allocator change.
   *
   * @param <A>
   *          indicates that for this instantiation of the allocator
   *
   * @param allocatorChange
   *          specify a callback object for allocator change
   */
  public <A extends CommonAllocator<A>> void setAllocatorChange(AllocatorChange allocatorChange) {
    m_allocatorChange = allocatorChange;
  }

  /**
   * a factory to create memory resource.
   * 
   * @param <H>
   *          the type of holder
   *
   * @param creator
   *          specify a creator to delegate concrete holder creation
   * 
   * @param startlevel
   *          specify a perf. level that is the start level for memory resource
   *          allocation
   * 
   * @param size
   *          specify a size to request memory resource
   * 
   * @return a new created holder held an allocated memory resource
   */
  protected <H extends MemHolder<? extends CommonAllocator<?>, ?, ?>> H create(MemCreate<H> creator,
      PerformanceLevel startlevel, long size) {
    H ret = null;
    boolean eligible = false;
    for (PerformanceLevel lvl : m_info.keySet()) {
      if (!eligible && startlevel == lvl) {
        eligible = true;
      }
      if (eligible) {
        int distance = 0;
        List<CommonAllocator<?>> bmas = m_info.get(lvl);
        for (CommonAllocator<?> bma : bmas) {
          ret = creator.create(bma, size);
          if (null == ret) {
            distance++;
          } else {
            if (null != m_bwlvlchange && m_prevbwlevel != lvl) {
              m_bwlvlchange.changed(m_prevbwlevel, lvl);
              m_prevbwlevel = lvl;
            }
            if (null != m_allocatorChange && m_prevallocator != bma) {
              m_allocatorChange.changed(lvl, m_prevallocator, bma);
              m_prevallocator = bma;
            }
            break;
          }
        }
        Collections.rotate(bmas, distance);
      }
      if (null != ret) {
        break;
      }
    }
    return ret;
  }

  /**
   * create a chunk from this clustering.
   * 
   * @param size
   *          specify the size of memory chunk
   * 
   * @return a holder with a created chunk
   * 
   */
  // @Override
  public MemChunkHolder<?> createChunk(long size) {
    return createChunk(PerformanceLevel.FASTEST, size);
  }

  /**
   * create a chunk from this clustering
   * 
   * @param startlevel
   *          specify the perf. level from which to search qualified node.
   * 
   * @param size
   *          specify the size of memory chunk
   * 
   * @return a holder with a created chunk
   */
  public MemChunkHolder<?> createChunk(PerformanceLevel startlevel, long size) {
    return create(m_memchunkcreate, startlevel, size);
  }

  /**
   * create a buffer from this clustering.
   * 
   * @param size
   *          specify the size of memory buffer
   * 
   * @return a holder with a created buffer
   * 
   */
  // @Override
  public MemBufferHolder<?> createBuffer(long size) {
    return createBuffer(PerformanceLevel.FASTEST, size);
  }

  /**
   * create a buffer from this clustering
   * 
   * @param startlevel
   *          specify the perf. level from which to search qualified node.
   * 
   * @param size
   *          specify the size of memory buffer
   * 
   * @return a holder with a created buffer
   */
  public MemBufferHolder<?> createBuffer(PerformanceLevel startlevel, long size) {
    return create(m_membuffercreate, startlevel, size);
  }

}
