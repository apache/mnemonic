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
 * Provide persist operations
 */
public interface Persistence<A extends RetrievableAllocator<A>> {

    /**
     * Make any cached changes to a memory resource persistent.
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
    void syncToNonVolatileMemory(long addr, long length, boolean autodetect);

    /**
     * flush processors cache for a memory resource
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
    void syncToLocal(long addr, long length, boolean autodetect);

    /**
     * persist a buffer to persistent memory.
     *
     * @param mbuf
     *         specify a buffer to be persisted
     */
    void syncToNonVolatileMemory(MemBufferHolder<A> mbuf);

    /**
     * persist a chunk to persistent memory.
     *
     * @param mchunk
     *         specify a chunk to be persisted
     */
    void syncToNonVolatileMemory(MemChunkHolder<A> mchunk);

    /**
     * persist the memory pool to persistent memory.
     */
    void persistAll();

    /**
     * flush a buffer to persistent memory.
     *
     * @param mbuf
     *         specify a buffer to be flushed
     */
    void syncToLocal(MemBufferHolder<A> mbuf);

    /**
     * flush a chunk to persistent memory.
     *
     * @param mchunk
     *         specify a chunk to be flushed
     */
    void syncToLocal(MemChunkHolder<A> mchunk);

    /**
     * flush the memory pool to persistent memory.
     */
    void flushAll();

    /**
     * drain memory caches to persistent memory.
     */
    void drain();

}
