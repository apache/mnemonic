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
import java.nio.ByteBuffer;

/**
 * DurableBuffer is a non-volatile buffer that supports persistence and
 * restoration.
 *
 * @param <A> The allocator type that implements RetrievableAllocator
 */
public class DurableBuffer<A extends RetrievableAllocator<A>> extends MemBufferHolder<A> implements Durable {
  
  // Persistence operations if the allocator supports it
  protected Persistence<A> m_persistOps = null;

  /**
   * Constructor for DurableBuffer.
   *
   * @param ar The allocator to be used.
   * @param mres The ByteBuffer resource.
   */
  @SuppressWarnings("unchecked")
  public DurableBuffer(A ar, ByteBuffer mres) {
    super(ar, mres);
    if (ar instanceof Persistence) {
      m_persistOps = (Persistence<A>) ar;
    }
  }

  /**
   * Called after creating the object to initialize it.
   */
  @Override
  public void initializeAfterCreate() {
    // Implementation for initializing after creation
  }

  /**
   * Called after restoring the object to initialize it.
   */
  @Override
  public void initializeAfterRestore() {
    // Implementation for initializing after restoration
  }

  /**
   * Setup generic information for the entity factory proxies and types.
   *
   * @param efproxies Array of entity factory proxies.
   * @param gftypes Array of generic field types.
   */
  @Override
  public void setupGenericInfo(EntityFactoryProxy[] efproxies, DurableType[] gftypes) {
    // Implementation for setting up generic information
  }

  /**
   * Register the buffer for automatic reclamation.
   *
   * @param rctx The reclaim context.
   */
  @Override
  public void registerAutoReclaim(ReclaimContext rctx) {
    super.registerAutoReclaim(rctx);
  }

  /**
   * Get the handler of the buffer.
   *
   * @return The buffer handler.
   */
  @Override
  public long getHandler() {
    return m_allocator.getBufferHandler(this);
  }

  /**
   * Synchronize the buffer to volatile memory.
   */
  @Override
  public void syncToVolatileMemory() {
    m_allocator.syncToVolatileMemory(this);
  }

  /**
   * Make any cached changes to the buffer persistent.
   */
  @Override
  public void syncToNonVolatileMemory() {
    if (m_persistOps != null) {
      m_persistOps.syncToNonVolatileMemory(this);
    }
  }

  /**
   * Flush processor cache for the buffer.
   */
  @Override
  public void syncToLocal() {
    if (m_persistOps != null) {
      m_persistOps.syncToLocal(this);
    }
  }

  /**
   * Get native field information.
   *
   * @return An array of native field information.
   */
  @Override
  public long[][] getNativeFieldInfo() {
    return null; // Implementation can be added if needed
  }

  /**
   * Break references held by this buffer.
   */
  @Override
  public void refbreak() {
    // Implementation for breaking references
  }

  /**
   * Example of an additional method: Clear the buffer.
   */
  public void clearBuffer() {
    ByteBuffer buffer = get();
    if (buffer != null) {
      buffer.clear();
    }
  }

  /**
   * Example of an additional method: Get buffer capacity.
   *
   * @return The capacity of the buffer.
   */
  public int getCapacity() {
    ByteBuffer buffer = get();
    return buffer != null ? buffer.capacity() : 0;
  }
}
