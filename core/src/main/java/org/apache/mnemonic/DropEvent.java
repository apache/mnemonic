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
 * A event listener to monitor and post-process an entry's evacuation.
 * 
 * 
 */
public interface DropEvent<KeyT, ValueT> {
  /**
   * A call-back actor when an entry has been evicted. a customized drop action
   * can be implemented on this interface's method e.g. spill this entry to disk
   * or release associated resources etc.
   * 
   * @param pool
   *          the pool which an entry has been evicted from
   * 
   * @param k
   *          the key of an entry that has been evicted
   * 
   * @param v
   *          the value of an entry that has been evicted
   */
  void drop(CachePool<KeyT, ValueT> pool, KeyT k, ValueT v);
}
