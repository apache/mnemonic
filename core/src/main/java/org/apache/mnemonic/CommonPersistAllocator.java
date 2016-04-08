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

/**
 * an abstract common class for persistent memory allocator to provide common
 * functionalities.
 *
 */
public abstract class CommonPersistAllocator<A extends CommonAllocator<A>> extends CommonAllocator<A> {

  /**
   * determine whether the allocator supports transaction feature or not
   *
   * @return true if supported
   */
  public boolean supportTransaction() {
    return false;
  }

  /**
   * determine whether the allocator does atomic operations on memory pool
   *
   * @return true if it is
   *
   */
  public boolean isAtomicOperation() {
    return false;
  }

  /**
   * determine whether this allocator supports to store non-volatile handler or
   * not
   *
   * @return true if there is
   */
  public boolean hasNonVolatileHandlerStore() {
    return false;
  }

  /**
   * retrieve a memory buffer from its backed memory allocator.
   * 
   * @param phandler
   *          specify the handler of memory buffer to retrieve
   *
   * @return a holder contains the retrieved memory buffer
   */
  public MemBufferHolder<A> retrieveBuffer(long phandler) {
    return retrieveBuffer(phandler, true);
  }

  /**
   * retrieve a memory chunk from its backed memory allocator.
   * 
   * @param phandler
   *          specify the handler of memory chunk to retrieve
   *
   * @return a holder contains the retrieved memory chunk
   */
  public MemChunkHolder<A> retrieveChunk(long phandler) {
    return retrieveChunk(phandler, true);
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
  abstract public MemBufferHolder<A> retrieveBuffer(long phandler, boolean autoreclaim);

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
  abstract public MemChunkHolder<A> retrieveChunk(long phandler, boolean autoreclaim);

  /**
   * get the handler from a memory buffer holder.
   * 
   * @param mbuf
   *          specify the memory buffer holder
   *
   * @return a handler that could be used to retrieve its memory buffer
   */
  abstract public long getBufferHandler(MemBufferHolder<A> mbuf);

  /**
   * get the handler from a memory chunk holder.
   * 
   * @param mchunk
   *          specify the memory chunk holder
   *
   * @return a handler that could be used to retrieve its memory chunk
   */
  abstract public long getChunkHandler(MemChunkHolder<A> mchunk);

  /**
   * start a application level transaction on this allocator.
   *
   */
  abstract public void beginTransaction();

  /**
   * end a application level transaction on this allocator.
   *
   */
  abstract public void endTransaction();

}
