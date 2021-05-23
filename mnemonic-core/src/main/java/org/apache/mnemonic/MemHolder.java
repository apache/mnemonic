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
import org.apache.mnemonic.resgc.ResHolder;

/**
 * hold a memory kind of resource.
 * 
 */
public abstract class MemHolder<A extends CommonAllocator<A>, T, H extends MemHolder<A, T, H>> extends ResHolder<T, H> {

  protected A m_allocator;

  /**
   * Constructor: initialize with resource.
   * 
   * @param mres
   *          specify a resource to be holden
   * 
   * @param ar
   *          specify an Allocator for this holder
   * 
   */
  public MemHolder(T mres, A ar) {
    super(mres);
    m_allocator = ar;
  }

  /**
   * get its allocator
   *
   * @return the allocator
   */
  public A getAllocator() {
    return m_allocator;
  }

  /**
   * resize its held resource
   *
   * @param size
   *          specify the new size for its held resource
   *
   * @return the holder of resource
   */
  public abstract MemHolder<A, T, H> resize(long size);

  /**
   * get the size of its held memory resource.
   * 
   * @return the size
   */
  public abstract long getSize();

  /**
   * register its held resource for auto-reclaim
   *
   */
  public abstract void registerAutoReclaim();

  /**
   * register its held resource for auto-reclaim
   *
   * @param rctx
   *          specify a reclaim context to register
   */
  public abstract void registerAutoReclaim(ReclaimContext rctx);
}
