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

package org.apache.mnemonic.service.memory;

import org.apache.mnemonic.resgc.ReclaimContext;

import java.nio.ByteBuffer;
import java.util.Set;

public interface VolatileMemoryAllocatorService extends QueryableService {


  /**
   * retrieve a bytebuffer from its handler
   *
   * @param id
   *          the identifier of backed memory pool
   * 
   * @param handler
   *          the handler of a nonvolatile bytebuffer
   *
   * @return the nonvolatile bytebuffer
   *
   */
  ByteBuffer retrieveByteBuffer(long id, long handler);

  /**
   * retrieve the size of a nonvolatile memory object
   *
   * @param id
   *          the identifier of backed memory pool
   * 
   * @param handler
   *          the handler of a nonvolatile object
   *
   * @return the size of nonvolatile object
   *
   */
  long retrieveSize(long id, long handler);

  /**
   * get the handler of a nonvolatile bytebuffer
   *
   * @param id
   *          the identifier of backed memory pool
   * 
   * @param buf
   *          the nonvolatile bytebuffer
   *
   * @return the handler of this specified nonvolatile bytebuffer
   *
   */
  long getByteBufferHandler(long id, ByteBuffer buf);

  /**
   * set a handler to a key.
   * 
   * @param id
   *          the identifier of backed memory pool
   * 
   * @param key
   *          the key to set this handler
   * 
   * @param handler
   *          the handler
   */
  void setHandler(long id, long key, long handler);

  /**
   * get a handler from specified key.
   * 
   * @param id
   *          the identifier of backed memory pool
   * 
   * @param key
   *          the key to get its handler
   * 
   * @return the handler of the specified key
   */
  long getHandler(long id, long key);

  /**
   * return the number of available keys to use.
   * 
   * @param id
   *          the identifier of backed memory pool
   * 
   * @return the number of keys
   */
  long handlerCapacity(long id);

  /**
   * return the base address of this persistent memory pool.
   * 
   * @param id
   *          the identifier of backed memory pool
   * 
   * @return the base address of this pmem pool
   */
  long getBaseAddress(long id);

  /**
   * Provide the service identifier for this allocator
   *
   * @return the service identifier of this allocator
   */
  String getServiceId();

  /**
   * Initialize a memory pool through native interface backed by native library.
   * 
   * @param capacity
   *          the capacity of memory pool
   * 
   * @param uri
   *          the location of memory pool will be created
   * 
   * @param isnew
   *          a place holder, always specify it as true
   *
   * @return the identifier of created memory pool
   */
  long init(long capacity, String uri, boolean isnew);

  /**
   * Adjust the capacity of a memory pool
   *
   * @param id
   *         specify the id of underlying native allocator
   *
   * @param reserve
   *         reserve certain amount of memory space in tail for use
   *
   * @return the new capacity
   */
  long adjustCapacity(long id, long reserve);

  /**
   * close the memory pool through native interface.
   *
   * @param id
   *         specify the id of underlying native allocator
   */
  void close(long id);

  /**
   * force to synchronize an uncommitted data to backed memory pool.
   *
   * @param id
   *         specify the id of underlying native allocator
   * 
   * @param addr
   *          the address of a memory resource
   * 
   * @param length
   *          the length of the memory resource
   * 
   * @param autodetect
   *          if NULL == address and autodetect : sync. whole pool
   *          if 0L == length and autodetect : sync. block
   */
  void syncToVolatileMemory(long id, long addr, long length, boolean autodetect);

  /**
   * get the capacity of its managed memory space
   *
   * @param id
   *         specify the id of underlying native allocator
   *
   * @return the capacity of this allocator managed memory resource/device
   */
  long capacity(long id);

  /**
   * allocate specified size of memory block from backed memory pool.
   * 
   * @param id
   *          the identifier of backed memory pool
   * 
   * @param size
   *          specify size of memory block to be allocated
   *
   * @param initzero
   *          indicate if initialize it with zeros
   *
   * @return the address of allocated memory block from native memory pool
   */
  long allocate(long id, long size, boolean initzero);

  /**
   * reallocate a specified size of memory block from backed memory pool.
   * 
   * @param id
   *          the identifier of backed memory pool
   * 
   * @param addr
   *          the address of previous allocated memory block. it can be null.
   * 
   * @param size
   *          specify new size of memory block to be reallocated
   * 
   * @param initzero
   *          indicate if initialize it with zeros
   *
   * @return the address of reallocated memory block from native memory pool
   */
  long reallocate(long id, long addr, long size, boolean initzero);

  /**
   * free a memory block by specify its address into backed memory pool.
   * 
   * @param id
   *          the identifier of backed memory pool
   * 
   * @param addr
   *          the address of allocated memory block.
   *
   * @param rctx
   *          the reclaim context.
   */
  void free(long id, long addr, ReclaimContext rctx);

  /**
   * create a ByteBuffer object which backed buffer is coming from backed native
   * memory pool.
   * 
   * @param id
   *          the identifier of backed memory pool
   * 
   * @param size
   *          the size of backed buffer that is managed by created ByteBuffer
   *          object.
   * 
   * @return a created ByteBuffer object with a backed native memory block
   */
  ByteBuffer createByteBuffer(long id, long size);

  /**
   * resize a ByteBuffer object which backed buffer is coming from backed native
   * memory pool. NOTE: the ByteBuffer object will be renewed and lost metadata
   * e.g. position, mark and etc.
   * 
   * @param id
   *          the identifier of backed memory pool
   * 
   * @param bytebuf
   *          the specified ByteBuffer object to be destroyed
   * 
   * @param size
   *          the new size of backed buffer that is managed by created
   *          ByteBuffer object.
   * 
   * @return a created ByteBuffer object with a backed native memory block
   */
  ByteBuffer resizeByteBuffer(long id, ByteBuffer bytebuf, long size);

  /**
   * destroy a native memory block backed ByteBuffer object.
   * 
   * @param id
   *          the identifier of backed memory pool
   * 
   * @param bytebuf
   *          the specified ByteBuffer object to be destroyed
   *
   * @param rctx
   *          the reclaim context.
   */
  void destroyByteBuffer(long id, ByteBuffer bytebuf, ReclaimContext rctx);

  /**
   * begin a transaction
   * @param readOnly
   *          specify if the transaction is readonly
   */
  void beginTransaction(boolean readOnly);

  /**
   * commit current transaction
   */
  void commitTransaction();

  /**
   * abort current transaction
   */
  void abortTransaction();

  /**
   * determine if in a transaction
   * @return the true if it is in a transaction
   */
  boolean isInTransaction();

  /**
   * provide a set of features that the memory service can offer
   *
   * @return a set of features that supported by this memory service
   */
  Set<MemoryServiceFeature> getFeatures();

  /**
   * calculate the abstract address from portable address
   *
   * @param addr
   *          the portable address
   *
   * @return the abstract address
   */
  byte[] getAbstractAddress(long addr);

  /**
   * translate the portable address
   *
   * @param addr
   *          the address to be translated
   *
   * @return the portable address
   */
  long getPortableAddress(long addr);

  /**
   * translate the effective address
   *
   * @param addr
   *          the address to be translated
   *
   * @return the effective address
   */
  long getEffectiveAddress(long addr);

  /**
   * retrieve an array of memory functions
   *
   * @return an memory function array
   */
  long[] getMemoryFunctions();

}
