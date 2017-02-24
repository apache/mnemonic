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

public abstract class RetrievableAllocator<A extends RetrievableAllocator<A>> extends CommonAllocator<A>
  implements AddressTranslator, HandlerStore, Transaction {

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
  public abstract DurableChunk<A> createChunk(long size, boolean autoreclaim);

  /**
   * create a durable buffer that is managed by its holder.
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
  public abstract DurableBuffer<A> createBuffer(long size, boolean autoreclaim);

  /**
   * create a durable chunk that is managed by its holder.
   *
   * @param size
   *          specify the size of memory chunk
   *
   * @return a durable chunk contains a memory chunk
   */
  @Override
  public DurableChunk<A> createChunk(long size) {
    return createChunk(size, true);
  }

  /**
   * create a durable buffer that is managed by its holder.
   * 
   * @param size
   *          specify the size of memory buffer
   * 
   * @return a durable buffer contains a memory buffer
   */
  @Override
  public DurableBuffer<A> createBuffer(long size) {
    return createBuffer(size, true);
  }

  /**
   * retrieve a memory buffer from its backed memory allocator.
   * 
   * @param phandler
   *          specify the handler of memory buffer to retrieve
   *
   * @return a durable buffer contains the retrieved memory buffer
   */
  public DurableBuffer<A> retrieveBuffer(long phandler) {
    return retrieveBuffer(phandler, true);
  }

  /**
   * retrieve a memory chunk from its backed memory allocator.
   * 
   * @param phandler
   *          specify the handler of memory chunk to retrieve
   *
   * @return a durable chunk contains the retrieved memory chunk
   */
  public DurableChunk<A> retrieveChunk(long phandler) {
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
   * @return a durable buffer contains the retrieved memory buffer
   */
  public abstract DurableBuffer<A> retrieveBuffer(long phandler, boolean autoreclaim);

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
   * @return a durable chunk contains the retrieved memory chunk
   */
  public abstract DurableChunk<A> retrieveChunk(long phandler, boolean autoreclaim);

  /**
   * get the address from a memory buffer holder.
   * 
   * @param mbuf
   *          specify the memory buffer holder
   *
   * @return an address that could be used to retrieve its memory buffer
   */
  public abstract long getBufferAddress(MemBufferHolder<A> mbuf);

  /**
   * get the address from a memory chunk holder.
   * 
   * @param mchunk
   *          specify the memory chunk holder
   *
   * @return an address that could be used to retrieve its memory chunk
   */
  public abstract long getChunkAddress(MemChunkHolder<A> mchunk);

  /**
   * get the handler from a memory buffer holder.
   * 
   * @param mbuf
   *          specify the memory buffer holder
   *
   * @return a handler that could be used to retrieve its memory buffer
   */
  public long getBufferHandler(MemBufferHolder<A> mbuf) {
    return getPortableAddress(getBufferAddress(mbuf));
  }

  /**
   * get the handler from a memory chunk holder.
   * 
   * @param mchunk
   *          specify the memory chunk holder
   *
   * @return a handler that could be used to retrieve its memory chunk
   */
  public long getChunkHandler(MemChunkHolder<A> mchunk) {
    return getPortableAddress(getChunkAddress(mchunk));
  }

}
