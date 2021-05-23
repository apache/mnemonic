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
 * holder for a ByteBuffer instance.
 * 
 */
public class MemBufferHolder<A extends CommonAllocator<A>> extends MemHolder<A, ByteBuffer, MemBufferHolder<A>> {

  /**
   * Constructor: initialize with a bytebuffer.
   * 
   * @param ar
   *          specify an Allocator for this holder
   * 
   * @param mres
   *          specify a chunk to be holden
   * 
   */
  public MemBufferHolder(A ar, ByteBuffer mres) {
    super(mres, ar);
  }

  /**
   * get the size of its held bytebuffer
   * 
   * @return the size
   */
  @Override
  public long getSize() {
    return m_mres.capacity();
  }

  /**
   * resize its held buffer
   *
   * @param size
   *          specify the new size for its held buffer
   */
  @Override
  public MemBufferHolder<A> resize(long size) {
    return m_allocator.resizeBuffer(this, size);
  }

  /**
   * register its held buffer for auto-reclaim
   *
   */
  @Override
  public void registerAutoReclaim() {
    m_allocator.registerBufferAutoReclaim(this);
  }

  /**
   * register its held buffer for auto-reclaim
   *
   * @param rctx
   *          specify a reclaim context to register
   */
  @Override
  public void registerAutoReclaim(ReclaimContext rctx) {
    m_allocator.registerBufferAutoReclaim(this, rctx);
  }

}
