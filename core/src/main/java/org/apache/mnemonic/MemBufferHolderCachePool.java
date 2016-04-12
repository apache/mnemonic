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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 
 * This class inherited from abstract CachePool class that implemented all
 * inherited abstract methods. it specialized to cache MemBufferHolder objects
 * that is backed by native memory pool.
 * 
 */
public class MemBufferHolderCachePool<KeyT> extends CachePool<KeyT, MemBufferHolder<?>> {

  private static final long serialVersionUID = 684275993324558070L;

  private AtomicLong currentMemory = new AtomicLong(0L);

  private long maxStoreCapacity = 0L;

  /**
   * Constructs an empty cache pool with specified capacity.
   * 
   * @param maxCapacity
   *          the capacity of this cache pool
   * 
   */
  public MemBufferHolderCachePool(long maxCapacity) {
    super(512, 0.75f);
    maxStoreCapacity = maxCapacity;
  }

  /**
   * @see CachePool#freeCapacity()
   */
  @Override
  public long freeCapacity() {
    return maxStoreCapacity - currentMemory.get();
  }

  /**
   * @see CachePool#usedCapacity()
   */
  @Override
  public long usedCapacity() {
    return currentMemory.get();
  }

  /**
   * @see Map#remove(Object)
   */
  @Override
  public synchronized MemBufferHolder<?> remove(Object k) {
    MemBufferHolder<?> ret = super.remove(k);
    if (null != ret) {
      currentMemory.getAndAdd(-ret.getSize());
    }
    return ret;
  }

  /**
   * Associates the specified value with the specified key in this map (optional
   * operation). If the map previously contained a mapping for the key, the old
   * value is replaced by the specified value. (A map m is said to contain a
   * mapping for a key k if and only if m.containsKey(k) would return true.)
   * 
   * @param k
   *          key with which the specified value is to be associated
   * 
   * @param v
   *          MemBufferHolder value to be associated with the specified key
   * 
   * @return the previous value associated with key, or null if there was no
   *         mapping for key. (A null return can also indicate that the map
   *         previously associated null with key, if the implementation supports
   *         null values.)
   */
  @Override
  public MemBufferHolder<?> put(KeyT k, MemBufferHolder<?> v) {
    return put(k, v, null, null);
  }

  /**
   * @see CachePool#put(Object, Object, DropEvent, EvictFilter)
   */
  @Override
  public MemBufferHolder<?> put(KeyT k, MemBufferHolder<?> v, DropEvent<KeyT, MemBufferHolder<?>> fsop,
      EvictFilter<KeyT, MemBufferHolder<?>> dfilter) {
    MemBufferHolder<?> ret = null;
    long sz = v.getSize();
    if (containsKey(k)) {
      sz -= get(k).getSize();
    }
    if (sz <= maxStoreCapacity && ensureFreeSpace(sz, fsop, dfilter)) {
      currentMemory.addAndGet(sz);
      ret = super.put(k, v);
    } else {
      throw new ContainerOverflowException("Out of capacity of MemBufferHolderCachePool.");
    }
    return ret;
  }

  /**
   * @see Map#putAll(Map)
   */
  @Override
  public void putAll(Map<? extends KeyT, ? extends MemBufferHolder<?>> m) {
    putAll(m, null, null);
  }

  /**
   * @see CachePool#putAll(Map, DropEvent, EvictFilter)
   */
  @Override
  public void putAll(Map<? extends KeyT, ? extends MemBufferHolder<?>> m, DropEvent<KeyT, MemBufferHolder<?>> fsop,
      EvictFilter<KeyT, MemBufferHolder<?>> dfilter) {

    long reqsz = 0;
    for (KeyT k : m.keySet()) {
      reqsz += m.get(k).getSize();
    }

    if (reqsz <= maxStoreCapacity && ensureFreeSpace(reqsz, fsop, dfilter)) {
      currentMemory.addAndGet(reqsz);
      super.putAll(m);
    } else {
      throw new ContainerOverflowException("Out of capacity of MemBufferHolderCachePool.");
    }
  }

  /**
   * @see CachePool#hotKeySet(int)
   */
  @Override
  public Set<KeyT> hotKeySet(int n) {
    Set<KeyT> ret = new HashSet<KeyT>();
    ArrayList<KeyT> keys = new ArrayList<KeyT>(keySet());
    int endindex = keys.size() > n ? keys.size() - n : 0;
    for (int i = keys.size(); i > endindex; i--) {
      ret.add(keys.get(i - 1));
    }
    return ret;
  }

  /**
   * @see CachePool#ensureFreeSpace(long)
   */
  @Override
  public boolean ensureFreeSpace(long freesz) {
    return ensureFreeSpace(freesz, null, null);
  }

  /**
   * @see CachePool#removeFirstEntry(DropEvent, EvictFilter)
   */
  @Override
  public boolean removeFirstEntry(DropEvent<KeyT, MemBufferHolder<?>> fsop,
      EvictFilter<KeyT, MemBufferHolder<?>> dfilter) {
    boolean ret = false;
    boolean delible = true;
    for (Map.Entry<KeyT, MemBufferHolder<?>> entry : entrySet()) {
      if (null != dfilter) {
        delible = dfilter.validate(this, entry.getKey(), entry.getValue());
      }
      if (delible) {
        KeyT k = entry.getKey();
        MemBufferHolder<?> v = remove(k);
        if (null != fsop) {
          fsop.drop(this, k, v);
        }
        ret = true;
        break;
      }
    }
    return ret;
  }

  /**
   * @see CachePool#ensureFreeSpace(long, DropEvent, EvictFilter)
   */
  @Override
  public boolean ensureFreeSpace(long freesz, DropEvent<KeyT, MemBufferHolder<?>> fsop,
      EvictFilter<KeyT, MemBufferHolder<?>> dfilter) {
    boolean ret = false;

    if (freesz <= freeCapacity()) {
      return true;
    }

    if (freesz > maxStoreCapacity) {
      return false;
    }

    long selectedMemory = 0L;
    Set<KeyT> selected = new HashSet<KeyT>();

    boolean delible = true;
    for (Map.Entry<KeyT, MemBufferHolder<?>> entry : entrySet()) {
      if (null != dfilter) {
        delible = dfilter.validate(this, entry.getKey(), entry.getValue());
      }
      if (delible) {
        selectedMemory += entry.getValue().getSize();
        selected.add(entry.getKey());
        if (freesz <= freeCapacity() + selectedMemory) {
          ret = true;
          break;
        }
      }
    }
    if (ret) {
      for (KeyT k : selected) {
        MemBufferHolder<?> mbh = remove(k);
        if (null != fsop) {
          fsop.drop(this, k, mbh);
        }
      }
    }
    return ret;
  }

}
