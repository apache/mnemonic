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

import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This is a abstract CachePool class that is inherited from LinedHashMap class,
 * it extends functionalities of its parent class to support some new features
 * that is usually requried in data caching usage scenario.
 *
 *
 */
public abstract class CachePool<KeyT, ValueT> extends LinkedHashMap<KeyT, ValueT> {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public CachePool(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  /**
   * Return available capacity for new entries.
   *
   * @return available capacity in this cache pool
   */
  public abstract long freeCapacity();

  /**
   * Return the used capacity of cached entries.
   *
   * @return the used size of this cache pool
   */
  public abstract long usedCapacity();

  /**
   * Put an entry in this cache pool and evict eldest entries if necessary that
   * will free enough space to hold new entry, which entry could be evicted that
   * can be customized by ({@link EvictFilter}), regarding how to post-process
   * the eldest entry that can be customized by ( {@link DropEvent}). If this
   * cache pool previously contained an entry for the key, the old value will be
   * replaced by the specified value
   *
   * @param k
   *          the key whoes associated value is to be put
   *
   * @param v
   *          the value to be put
   *
   * @param fsop
   *          the customized operations to free space to hold new entry
   *
   * @param dfilter
   *          the filter of entries for deletion
   *
   * @return <code>true</code> if the entry has been put into this container
   *
   */
  public abstract ValueT put(KeyT k, ValueT v, DropEvent<KeyT, ValueT> fsop, EvictFilter<KeyT, ValueT> dfilter);

  /**
   * Put all entries into this cache pool and evict eldes entries if necessary.
   *
   * @param m
   *          the Map object that contains entries to be put
   *
   * @param fsop
   *          the customized operations to free space to hold new entry
   *
   * @param dfilter
   *          the filter of entries for deletion
   *
   */
  public abstract void putAll(Map<? extends KeyT, ? extends ValueT> m, DropEvent<KeyT, ValueT> fsop,
      EvictFilter<KeyT, ValueT> dfilter);

  /**
   * Returns a new {@link Set} view of the keys of this cache pool, It contains
   * the most recently visited keys
   *
   * @param n
   *          the number of keys to retrieve
   *
   * @return a set of hot keys
   */
  public abstract Set<KeyT> hotKeySet(int n);

  /**
   * Ensure the free capacity is greater than the specified size
   *
   * @param freesz
   *          the size of free capacity that needs to be secured
   *
   * @return <code>true</code> if the size of free capacity is greater than the
   *         specified size after evacuation
   *
   * @see #ensureFreeSpace(long, DropEvent, EvictFilter)
   */
  public abstract boolean ensureFreeSpace(long freesz);

  /**
   * Removes a first qualified entry in this cache pool
   *
   * @param fsop
   *          the customized callback to post-process its evicted entry
   *
   * @param dfilter
   *          the filter for entry deletion
   *
   * @return <code>true</code> if there is one qualified entry that has been dropped
   */
  public abstract boolean removeFirstEntry(DropEvent<KeyT, ValueT> fsop, EvictFilter<KeyT, ValueT> dfilter);

  /**
   * Ensure the size of free capacity is greater than the specified size, the
   * entries will be filtered by {@link EvictFilter} before dropping, the
   * {@link DropEvent} is used for post-processing
   *
   * @param freesz
   *          the size of free capacity that needs to be secured
   *
   * @param fsop
   *          the customized operations to free space to hold new entry
   *
   * @param dfilter
   *          the filter of entries for deletion
   *
   * @return <code>true</code> if the size of free capacity is greater than the
   *         specified size after evication if necessary
   */
  public abstract boolean ensureFreeSpace(long freesz, DropEvent<KeyT, ValueT> fsop, EvictFilter<KeyT, ValueT> dfilter);

}
