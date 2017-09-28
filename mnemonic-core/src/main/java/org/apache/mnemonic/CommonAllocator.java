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

import org.apache.mnemonic.service.memory.MemoryServiceFeature;
import org.flowcomputing.commons.resgc.ResCollector;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * an abstract common class for memory allocator to provide common
 * functionalities.
 * 
 */
public abstract class CommonAllocator<A extends CommonAllocator<A>> implements Allocator<A> {

  protected Reclaim<Long> m_chunkreclaimer = null;
  protected Reclaim<ByteBuffer> m_bufferreclaimer = null;

  protected ResCollector<MemChunkHolder<A>, Long> m_chunkcollector = null;
  protected ResCollector<MemBufferHolder<A>, ByteBuffer> m_bufcollector = null;

  protected Set<MemoryServiceFeature> m_features = null;
  protected boolean m_absaddr = false;

  public boolean useAbstractAddressing() {
    return m_absaddr;
  }

  /**
   * set a reclaimer to reclaim memory buffer
   * 
   * @param reclaimer
   *          specify a reclaimer to accept reclaim request
   */
  public void setBufferReclaimer(Reclaim<ByteBuffer> reclaimer) {
    m_bufferreclaimer = reclaimer;
  }

  /**
   * set a reclaimer to reclaim memory chunk
   * 
   * @param reclaimer
   *          specify a reclaimer to accept reclaim request
   */
  public void setChunkReclaimer(Reclaim<Long> reclaimer) {
    m_chunkreclaimer = reclaimer;
  }

  /**
   * create a memory chunk that is managed by its holder.
   * 
   * @param size
   *          specify the size of memory chunk
   * 
   * @return a holder contains a memory chunk
   */
  @Override
  public MemChunkHolder<A> createChunk(long size) {
    return createChunk(size, true);
  }

  /**
   * create a memory buffer that is managed by its holder.
   * 
   * @param size
   *          specify the size of memory buffer
   * 
   * @return a holder contains a memory buffer
   */
  @Override
  public MemBufferHolder<A> createBuffer(long size) {
    return createBuffer(size, true);
  }

  /**
   * register a memory chunk for auto-reclaim
   *
   * @param mholder
   *          specify a chunk holder to register
   */
  @Override
  public void registerChunkAutoReclaim(MemChunkHolder<A> mholder) {
    m_chunkcollector.register(mholder);
  }

  /**
   * register a memory buffer for auto-reclaim
   *
   * @param mholder
   *          specify a buffer holder to register
   */
  @Override
  public void registerBufferAutoReclaim(MemBufferHolder<A> mholder) {
    m_bufcollector.register(mholder);
  }

  /**
   * close both of resource collectors for this allocator
   *
   */
  @Override
  public void close() {
    if (null != m_chunkcollector) {
      m_chunkcollector.close();
      m_chunkcollector = null;
    }
    if (null != m_bufcollector) {
      m_bufcollector.close();
      m_bufcollector = null;
    }
  }

}
