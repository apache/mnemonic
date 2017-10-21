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
 * an interface to manage the lifecycle of memory allocator
 *
 */
public interface Allocator<A extends CommonAllocator<A>> extends Allocatable<A> {

  /**
   * release the underlying memory pool and close it.
   * 
   */
  void close();

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
   *          detect the length of this memory block
   *
   */
  void syncToVolatileMemory(long addr, long length, boolean autodetect);

  /**
   * sync. a buffer to memory.
   *
   * @param mbuf
   *         specify a buffer to be sync.
   */
  void syncToVolatileMemory(MemBufferHolder<A> mbuf);

  /**
   * sync. a chunk to memory.
   *
   * @param mchunk
   *         specify a chunk to be sync.
   */
  void syncToVolatileMemory(MemChunkHolder<A> mchunk);

  void syncAll();

  /**
   * enable active garbage collection. the GC will be forced to collect garbages
   * when there is no more space for current allocation request.
   *
   * @param timeout
   *          the timeout is used to yield for GC performing
   *
   * @return this allocator
   */
  A enableActiveGC(long timeout);

  /**
   * disable active garbage collection.
   *
   * @return this allocator
   */
  A disableActiveGC();

}
