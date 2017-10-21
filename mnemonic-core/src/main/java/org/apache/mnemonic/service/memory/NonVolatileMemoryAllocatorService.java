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

import java.nio.ByteBuffer;

public interface NonVolatileMemoryAllocatorService extends VolatileMemoryAllocatorService {

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
   * @param addr
   *          the address of a nonvolatile object
   *
   * @return the size of nonvolatile object
   *
   */
  long retrieveSize(long id, long addr);

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
   * Make any cached changes to a memory resource persistent.
   * 
   * @param id
   *          the identifier of backed memory pool
   * 
   * @param addr
   *          the address of a memory resource
   * 
   * @param length
   *          the length of the memory resource
   * 
   * @param autodetect
   *          if NULL == address and autodetect : persist whole pool
   *          if 0L == length and autodetect : persist block
   */
  void syncToNonVolatileMemory(long id, long addr, long length, boolean autodetect);

  /**
   * flush processors cache for a memory resource
   * 
   * @param id
   *          the identifier of backed memory pool
   * 
   * @param addr
   *          the address of a memory resource
   * 
   * @param length
   *          the length of the memory resource
   * 
   * @param autodetect
   *          if NULL == address and autodetect : flush whole pool
   *          if 0L == length and autodetect : flush block
   */
  void syncToLocal(long id, long addr, long length, boolean autodetect);

  /**
   * wait for any memory resource stores to drain from HW buffers.
   * 
   * @param id
   *          the identifier of backed memory pool
   */
  void drain(long id);

  /**
   * return the base address of this persistent memory pool.
   * 
   * @param id
   *          the identifier of backed memory pool
   * 
   * @return the base address of this pmem pool
   */
  long getBaseAddress(long id);

}
