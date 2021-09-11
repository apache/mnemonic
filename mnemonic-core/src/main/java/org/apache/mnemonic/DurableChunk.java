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

import org.apache.mnemonic.resgc.ReclaimContext;

public class DurableChunk<A extends RetrievableAllocator<A>> extends MemChunkHolder<A> implements Durable {
  protected Persistence<A> m_persistOps = null;

  @SuppressWarnings("unchecked")
  public DurableChunk(A ar, Long mres, long size) {
    super(ar, mres, size);
    if (ar instanceof Persistence) {
      m_persistOps = (Persistence<A>) ar;
    }
  }

  @Override
  public void initializeAfterCreate() {
  }

  @Override
  public void initializeAfterRestore() {
  }

  @Override
  public void setupGenericInfo(EntityFactoryProxy[] efproxies, DurableType[] gftypes) {
  }

  @Override
  public void registerAutoReclaim(ReclaimContext rctx) {
    super.registerAutoReclaim(rctx);
  }

  @Override
  public long getHandler() {
    return m_allocator.getChunkHandler(this);
  }

  /**
   * sync. this object
   */
  @Override
  public void syncToVolatileMemory() {
    m_allocator.syncToVolatileMemory(this);
  }

  /**
   * Make any cached changes to this object persistent.
   */
  @Override
  public void syncToNonVolatileMemory() {
    if (null != m_persistOps) {
      m_persistOps.syncToNonVolatileMemory(this);
    }
  }

  /**
   * flush processors cache for this object
   */
  @Override
  public void syncToLocal() {
    if (null != m_persistOps) {
      m_persistOps.syncToLocal(this);
    }
  }

  @Override
  public long[][] getNativeFieldInfo() {
    return null;
  }

  @Override
  public void refbreak() {
    return;
  }

  /**
   * Get a buffer backed by a region of DurableChunk
   * @param offset
   *          the start position of region
   * @param size
   *          the size of region
   * @return
   *          the ChunkBuffer to present the region of chunk
   */
  @SuppressWarnings("unchecked")
  public ChunkBuffer getChunkBuffer(long offset, int size) {
    return new ChunkBuffer(this, offset, size);
  }

}
