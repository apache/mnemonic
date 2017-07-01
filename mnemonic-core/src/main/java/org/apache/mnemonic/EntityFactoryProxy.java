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
 * delegate the restoration/creation of generic non-volatile object fields
 *
 */

public interface EntityFactoryProxy {
  /**
   * create a durable object from persistent allocator using a handler of
   * non-volatile object
   *
   * @param <A>
   *          indicates that for this instantiation of the allocator.
   *
   * @param allocator
   *          specify a persistent allocator instance
   *
   * @param factoryproxys
   *          specify an array of factory proxies for its restored non-volatile
   *          object
   *
   * @param gfields
   *          specify an array of generic types of its generic fields
   *          corresponding to factory proxies
   *
   * @param autoreclaim
   *          specify auto-reclaim for this restored non-volatile object
   *
   * @return the created non-volatile object from this factory proxy
   *
   */
  <A extends RestorableAllocator<A>> Durable create(A allocator, EntityFactoryProxy[] factoryproxys,
      DurableType[] gfields, boolean autoreclaim);

  <A extends RestorableAllocator<A>> Durable create(ParameterHolder<A> ph);

  /**
   * restore a durable object from persistent allocator using a handler of
   * non-volatile object
   *
   * @param <A>
   *          indicates that for this instantiation of the allocator.
   *
   * @param allocator
   *          specify a persistent allocator instance
   *
   * @param factoryproxys
   *          specify an array of factory proxies for its restored non-volatile
   *          object
   *
   * @param gfields
   *          specify an array of generic types of its generic fields
   *          corresponding to factoryproxys
   *
   * @param phandler
   *          specify a non-volatile handler to restore
   *
   * @param autoreclaim
   *          specify auto-reclaim for this restored non-volatile object
   *
   * @return the restored non-volatile object from this factory proxy
   *
   */
  <A extends RestorableAllocator<A>> Durable restore(A allocator, EntityFactoryProxy[] factoryproxys,
      DurableType[] gfields, long phandler, boolean autoreclaim);

  <A extends RestorableAllocator<A>> Durable restore(ParameterHolder<A> ph);
}
