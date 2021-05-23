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

/**
 * an interface to allocate memory resources from any underlying memory kind of
 * storage.
 * 
 */
public interface Allocatable<A extends CommonAllocator<A>> {

  /**
   * create a memory chunk that is managed by its holder.
   *
   * @param size
   *          specify the size of memory chunk
   *
   * @return a holder contains a memory chunk
   */
  MemChunkHolder<A> createChunk(long size);

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
  MemChunkHolder<A> createChunk(long size, boolean autoreclaim);

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
  MemChunkHolder<A> createChunk(long size, boolean autoreclaim, ReclaimContext rctx);

  /**
   * create a memory buffer that is managed by its holder.
   *
   * @param size
   *          specify the size of memory buffer
   *
   * @return a holder contains a memory buffer
   */
  MemBufferHolder<A> createBuffer(long size);

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
  MemBufferHolder<A> createBuffer(long size, boolean autoreclaim);

  /**
   * create a memory buffer that is managed by its holder.
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
   * @return a holder contains a memory buffer
   */
  MemBufferHolder<A> createBuffer(long size, boolean autoreclaim, ReclaimContext rctx);

  /**
   * register a memory chunk for auto-reclaim
   *
   * @param mholder
   *          specify a chunk holder to register
   *
   * @param rctx
   *          specify a reclaim context
   */
  void registerChunkAutoReclaim(MemChunkHolder<A> mholder, ReclaimContext rctx);

    /**
     * register a memory chunk for auto-reclaim
     *
     * @param mholder
     *          specify a chunk holder to register
     */
  void registerChunkAutoReclaim(MemChunkHolder<A> mholder);

  /**
   * register a memory buffer for auto-reclaim
   *
   * @param mholder
   *          specify a buffer holder to register
   *
   * @param rctx
   *          specify a reclaim context
   */
  void registerBufferAutoReclaim(MemBufferHolder<A> mholder, ReclaimContext rctx);

  /**
   * register a memory buffer for auto-reclaim
   *
   * @param mholder
   *          specify a buffer holder to register
   */
  void registerBufferAutoReclaim(MemBufferHolder<A> mholder);

  /**
   * resize a memory chunk.
   * 
   * @param mholder
   *          specify a chunk holder for resizing
   * 
   * @param size
   *          specify a new size of this memory chunk
   * 
   * @return the resized memory chunk holder
   * 
   */
  MemChunkHolder<A> resizeChunk(MemChunkHolder<A> mholder, long size);

  /**
   * resize a memory buffer.
   * 
   * @param mholder
   *          specify a buffer holder for resizing
   * 
   * @param size
   *          specify a new size of this memory buffer
   * 
   * @return the resized memory buffer holder
   * 
   */
  MemBufferHolder<A> resizeBuffer(MemBufferHolder<A> mholder, long size);

}
