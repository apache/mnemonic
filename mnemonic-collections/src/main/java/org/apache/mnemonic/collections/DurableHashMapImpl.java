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

package org.apache.mnemonic.collections;

import org.apache.mnemonic.ConfigurationException;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.Durable;
import org.apache.mnemonic.EntityFactoryProxyHelper;
import org.apache.mnemonic.MemChunkHolder;
import org.apache.mnemonic.MemoryDurableEntity;
import org.apache.mnemonic.OutOfHybridMemory;
import org.apache.mnemonic.RestorableAllocator;
import org.apache.mnemonic.RestoreDurableEntityError;
import org.apache.mnemonic.resgc.ReclaimContext;
import org.apache.mnemonic.RetrieveDurableEntityError;
import org.apache.mnemonic.Utils;
import org.apache.commons.lang3.ArrayUtils;
import java.util.Iterator;
import java.util.NoSuchElementException;

@SuppressWarnings("restriction")
public class DurableHashMapImpl<A extends RestorableAllocator<A>, K, V>
        extends DurableHashMap<K, V> implements MemoryDurableEntity<A> {

  private static final long DEFAULT_MAP_SIZE = 16;
  private static final float DEFAULT_MAP_LOAD_FACTOR = 0.75f;
  private static final long MAX_OBJECT_SIZE = 8;
  private static long[][] fieldInfo;
  @SuppressWarnings({"restriction", "UseOfSunClasses"})
  private sun.misc.Unsafe unsafe;
  private EntityFactoryProxy[] factoryProxy;
  private EntityFactoryProxy[] listefproxies;
  private DurableType[] genericField;
  private DurableType[] listgftypes;
  private volatile boolean autoResize = true;
  private volatile boolean autoReclaim;
  private volatile ReclaimContext reclaimcontext;
  private MemChunkHolder<A> holder;
  private MemChunkHolder<A> chunkAddr;
  private A allocator;
  /**
   * Set initial capacity for a hashmap. It can grow in size.
   *
   * @param capacity
   *          Initial capacity to be set
   */
  public void setCapacityHint(long capacity) {
    if (0 == capacity) {
      totalCapacity = DEFAULT_MAP_SIZE;
    } else {
      totalCapacity = 1;
      while (totalCapacity < capacity) {
        totalCapacity <<= 1;
      }
    }
    threshold = (long) (totalCapacity * DEFAULT_MAP_LOAD_FACTOR);
  }

  /**
   * Add a new key-value pair to map
   *
   * @param key
   *          the key to be set
   *
   * @param value
   *          the value to be set
   *
   * @return previous value with key else return null
   */
  @Override
  public V put(K key, V value) throws OutOfHybridMemory {
    int hash = hash(key.hashCode());
    long bucketIndex = getBucketIndex(hash);
    long bucketAddr = holder.get() + MAX_OBJECT_SIZE * bucketIndex;
    V retVal = addEntry(key, value, bucketAddr);
    if (autoResize && (mapSize >= threshold)) {
      resize(2 * totalCapacity);
    }
    return retVal;
  }

  /**
   * Add a new key-value pair to map at a given bucket address
   *
   * @param key
   *          the key to be set
   *
   * @param value
   *          the value to be set
   *
   * @param bucketAddr
   *          the addr of the bucket where key is hashed
   *
   * @return previous value with key else return null
   */
  protected V addEntry(K key, V value, long bucketAddr) throws OutOfHybridMemory {
    V retValue = null;
    long handler = unsafe.getAddress(bucketAddr);
    if (0L == handler) {
      SinglyLinkedNode<MapEntry<K, V>> head = null;
      MapEntry<K, V> entry = null;
      try {
        head = SinglyLinkedNodeFactory.create(allocator, listefproxies, listgftypes, false, reclaimcontext);
        entry = MapEntryFactory.create(allocator, factoryProxy, genericField, false, reclaimcontext);
      } catch (OutOfHybridMemory fe) {
        if (null != head) {
          head.destroy();
        }
        if (null != entry) {
          entry.destroy();
        }
        throw fe;
      }
      entry.setKey(key, false);
      entry.setValue(value, false);
      head.setItem(entry, false);
      unsafe.putLong(bucketAddr, head.getHandler());
      mapSize++;
    } else {
      SinglyLinkedNode<MapEntry<K, V>> head = SinglyLinkedNodeFactory.restore(allocator,
          listefproxies, listgftypes, handler, false, reclaimcontext);
      SinglyLinkedNode<MapEntry<K, V>> prev = head;
      boolean found = false;
      while (null != head) {
        MapEntry<K, V> mapEntry = head.getItem();
        K entryKey = mapEntry.getKey();
        if (entryKey == key || entryKey.equals(key)) {
          retValue = mapEntry.getValue();
          if (retValue instanceof Durable) {
            mapEntry.setValue(value, false);
          } else {
            mapEntry.setValue(value, true);
          }
          found = true;
          break;
        }
        prev = head;
        head = head.getNext();
      }
      if (true != found) {
        SinglyLinkedNode<MapEntry<K, V>> newNode = null;
        MapEntry<K, V> entry = null;
        try {
          newNode = SinglyLinkedNodeFactory.create(allocator, listefproxies, listgftypes,
                  false, reclaimcontext);
          entry = MapEntryFactory.create(allocator, factoryProxy, genericField, false, reclaimcontext);
        } catch (OutOfHybridMemory fe) {
          if (null != newNode) {
            newNode.destroy();
          }
          if (null != entry) {
            entry.destroy();
          }
          throw fe;
        }
        entry.setKey(key, false);
        entry.setValue(value, false);
        newNode.setItem(entry, false);
        prev.setNext(newNode, false);
        mapSize++;
      }
    }
    return retValue;
  }

  /**
   * Return a value to which key is mapped
   *
   * @param key
   *          the key whose value is to be retrieved
   *
   * @return previous value with key else return null
   */
  @Override
  public V get(K key) {
    int hash = hash(key.hashCode());
    long bucketIndex = getBucketIndex(hash);
    long bucketAddr = holder.get() + MAX_OBJECT_SIZE * bucketIndex;
    return getEntry(key, bucketAddr);
  }

  /**
   * Return a value to which key is mapped given a bucket address
   *
   * @param key
   *          the key whose value is to be retrieved
   *
   * @param bucketAddr
   *          the addr of the bucket where key is hashed
   *
   * @return previous value with key else return null
   */
  protected V getEntry(K key, long bucketAddr) {
    V retValue = null;
    long handler = unsafe.getAddress(bucketAddr);
    if (0L != handler) {
      SinglyLinkedNode<MapEntry<K, V>> head = SinglyLinkedNodeFactory.restore(allocator,
          listefproxies, listgftypes, handler, false, reclaimcontext);
      while (null != head) {
        MapEntry<K, V> mapEntry = head.getItem();
        K entryKey = mapEntry.getKey();
        if (entryKey == key || entryKey.equals(key)) {
          retValue = mapEntry.getValue();
          break;
        }
        head = head.getNext();
      }
    }
    return retValue;
  }

  /**
   * Remove a mapping for a specified key
   *
   * @param key
   *          the key whose value is to be removed
   *
   * @return previous value with key else return null
   */
  @Override
  public V remove(K key) {
    int hash = hash(key.hashCode());
    long bucketIndex = getBucketIndex(hash);
    long bucketAddr = holder.get() + MAX_OBJECT_SIZE * bucketIndex;
    return removeEntry(key, bucketAddr);
  }

  /**
   * Remove a mapping for a specified key at given bucket address
   *
   * @param key
   *          the key whose value is to be removed
   *
   * @param bucketAddr
   *          the addr of the bucket where key is hashed
   *
   * @return previous value with key else return null
   */
  protected V removeEntry(K key, long bucketAddr) {
    V retValue = null;
    long handler = unsafe.getAddress(bucketAddr);
    if (0L != handler) {
      SinglyLinkedNode<MapEntry<K, V>> head = SinglyLinkedNodeFactory.restore(allocator,
          listefproxies, listgftypes, handler, false, reclaimcontext);
      SinglyLinkedNode<MapEntry<K, V>> prev = null;
      boolean found = false;
      while (null != head) {
        MapEntry<K, V> mapEntry = head.getItem();
        K entryKey = mapEntry.getKey();
        if (entryKey == key || entryKey.equals(key)) {
          retValue = mapEntry.getValue();
          found = true;
          break;
        }
        prev = head;
        head = head.getNext();
      }
      if (true == found) {
        if (null == prev) {
          if (null == head.getNext()) {
            unsafe.putAddress(bucketAddr, 0L);
            head.destroy();
          } else {
            unsafe.putAddress(bucketAddr, head.getNext().getHandler());
            head.destroy();
          }
        } else {
          prev.setNext(head.getNext(), false);
          head.destroy();
        }
        mapSize--;
      }
    }
    return retValue;
  }

  /**
   * Rehashes the entire map into a new map of given capacity
   *
   * @param newCapacity
   *          the capacity of new map
   */
  public void resize(long newCapacity) {
    MemChunkHolder<A> prevHolder = holder;
    long bucketAddr = prevHolder.get();
    long maxbucketAddr = bucketAddr + MAX_OBJECT_SIZE * totalCapacity;
    holder = allocator.createChunk(MAX_OBJECT_SIZE * newCapacity, autoReclaim);
    if (null == holder) {
      autoResize = false;
      holder = prevHolder;
      return;
    }
    totalCapacity = newCapacity;
    threshold = (long) (totalCapacity * DEFAULT_MAP_LOAD_FACTOR);
    unsafe.putLong(chunkAddr.get(), allocator.getChunkHandler(holder));
    while (bucketAddr < maxbucketAddr) {
      long handler = unsafe.getAddress(bucketAddr);
      if (0L != handler) {
        SinglyLinkedNode<MapEntry<K, V>> head = SinglyLinkedNodeFactory.restore(allocator,
            listefproxies, listgftypes, handler, false, reclaimcontext);
        SinglyLinkedNode<MapEntry<K, V>> curr = head;
        while (null != curr) {
          curr = curr.getNext();
          transfer(head);
          head = curr;
        }
      }
      bucketAddr += MAX_OBJECT_SIZE;
    }
    prevHolder.destroy();
  }

  /**
   * Transfers a map item from old map to the new map
   *
   * @param elem
   *          the item in the old map
   */
  protected void transfer(SinglyLinkedNode<MapEntry<K, V>> elem) {
    int hash = hash(elem.getItem().getKey().hashCode());
    long bucketIndex = getBucketIndex(hash);
    long bucketAddr = holder.get() + MAX_OBJECT_SIZE * bucketIndex;
    long handler = unsafe.getAddress(bucketAddr);
    if (0L != handler) {
      SinglyLinkedNode<MapEntry<K, V>> head = SinglyLinkedNodeFactory.restore(allocator,
          listefproxies, listgftypes, handler, false, reclaimcontext);
      elem.setNext(head, false);
    } else {
      elem.setNext(null, false);
    }
    unsafe.putLong(bucketAddr, elem.getHandler());
  }

  /**
   * Recomputes the size of the map during restore without persistence
   *
   *  @return size of the map
   */
  protected long recomputeMapSize() {
    long size = 0;
    long bucketAddr = holder.get();
    long maxbucketAddr = bucketAddr + MAX_OBJECT_SIZE * totalCapacity;
    while (bucketAddr < maxbucketAddr) {
      long handler = unsafe.getAddress(bucketAddr);
      if (0L != handler) {
        SinglyLinkedNode<MapEntry<K, V>> head = SinglyLinkedNodeFactory.restore(allocator,
            listefproxies, listgftypes, handler, false, reclaimcontext);
        while (null != head) {
          size++;
          head = head.getNext();
        }
      }
      bucketAddr += MAX_OBJECT_SIZE;
    }
    return size;
  }

  @Override
  public boolean autoReclaim() {
    return autoReclaim;
  }

  /**
   * sync. this object
   */
  @Override
  public void syncToVolatileMemory() {

  }

  /**
   * Make any cached changes to this object persistent.
   */
  @Override
  public void syncToNonVolatileMemory() {

  }

  /**
   * flush processors cache for this object
   */
  @Override
  public void syncToLocal() {

  }

  @Override
  public long[][] getNativeFieldInfo() {
    return fieldInfo;
  }

  @Override
  public void refbreak() {
    return;
  }

  @Override
  public void destroy() throws RetrieveDurableEntityError {
    long bucketAddr = holder.get();
    long maxbucketAddr = bucketAddr + MAX_OBJECT_SIZE * totalCapacity;
    SinglyLinkedNode<MapEntry<K, V>> head, prev;
    while (bucketAddr < maxbucketAddr) {
      long handler = unsafe.getAddress(bucketAddr);
      if (0L != handler) {
        head = SinglyLinkedNodeFactory.restore(allocator,
            listefproxies, listgftypes, handler, false, reclaimcontext);
        prev = head;
        while (null != head) {
          head = head.getNext();
          prev.destroy(); //TODO: Destroy head in a cascading way
          prev = head;
        }
      }
      bucketAddr += MAX_OBJECT_SIZE;
    }
    holder.destroy();
    chunkAddr.destroy();
  }

  @Override
  public void cancelAutoReclaim() {
    holder.cancelAutoReclaim();
    autoReclaim = false;
  }

  @Override
  public void registerAutoReclaim() {
    this.registerAutoReclaim(reclaimcontext);
  }

  @Override
  public void registerAutoReclaim(ReclaimContext rctx) {
    holder.registerAutoReclaim(rctx);
    autoReclaim = true;
    reclaimcontext = rctx;
  }

  @Override
  public long getHandler() {
    return allocator.getChunkHandler(chunkAddr);
  }

  @Override
  public void restoreDurableEntity(A allocator, EntityFactoryProxy[] factoryProxy,
             DurableType[] gField, long phandler, boolean autoReclaim, ReclaimContext rctx)
          throws RestoreDurableEntityError {
    initializeDurableEntity(allocator, factoryProxy, gField, autoReclaim, rctx);
    if (0L == phandler) {
      throw new RestoreDurableEntityError("Input handler is null on restoreDurableEntity.");
    }
    chunkAddr = allocator.retrieveChunk(phandler, autoReclaim, reclaimcontext);
    long chunkHandler = unsafe.getLong(chunkAddr.get());
    holder = allocator.retrieveChunk(chunkHandler, autoReclaim, reclaimcontext);
    if (null == holder || null == chunkAddr) {
      throw new RestoreDurableEntityError("Retrieve Entity Failure!");
    }
    setCapacityHint(holder.getSize() / MAX_OBJECT_SIZE);
    mapSize = recomputeMapSize();
    initializeAfterRestore();
  }


  @Override
  public void initializeDurableEntity(A allocator, EntityFactoryProxy[] factoryProxy,
              DurableType[] gField, boolean autoReclaim, ReclaimContext rctx) {
    this.allocator = allocator;
    this.factoryProxy = factoryProxy;
    this.genericField = gField;
    this.autoReclaim = autoReclaim;
    this.reclaimcontext = rctx;
    DurableType gftypes[] = {DurableType.DURABLE};
    this.listgftypes = ArrayUtils.addAll(gftypes, genericField);
    EntityFactoryProxy efproxies[] = new EntityFactoryProxy[0];
    try {
      efproxies = new EntityFactoryProxy[]{
          new EntityFactoryProxyHelper<MapEntry>(MapEntry.class, 1, reclaimcontext)};
    } catch (ClassNotFoundException e) {
      throw new ConfigurationException("Class MapEntry not found");
    } catch (NoSuchMethodException e) {
      throw new ConfigurationException("The methods of class MapEntry not found");
    }
    this.listefproxies = ArrayUtils.addAll(efproxies, factoryProxy);
    try {
      this.unsafe = Utils.getUnsafe();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void createDurableEntity(A allocator, EntityFactoryProxy[] factoryProxy,
              DurableType[] gField, boolean autoReclaim, ReclaimContext rctx) throws OutOfHybridMemory {
    initializeDurableEntity(allocator, factoryProxy, gField, autoReclaim, rctx);
    this.holder = allocator.createChunk(MAX_OBJECT_SIZE * totalCapacity, autoReclaim, reclaimcontext);
    this.chunkAddr = allocator.createChunk(MAX_OBJECT_SIZE, autoReclaim, reclaimcontext);
    unsafe.putLong(chunkAddr.get(), allocator.getChunkHandler(holder));
    if (null == this.holder || null == this.chunkAddr) {
      throw new OutOfHybridMemory("Create Durable Entity Error!");
    }
    initializeAfterCreate();
  }

  @Override
  public Iterator<MapEntry<K, V>> iterator() {
    return new HashMapItr(this);
  }

  private class HashMapItr implements Iterator<MapEntry<K, V>> {
    long currentBucketAddr = 0;
    long prevBucketAddr = 0;
    long maxBucketAddr = 0;
    DurableHashMapImpl<A, K, V> map;
    SinglyLinkedNode<MapEntry<K, V>> currentNode = null;
    SinglyLinkedNode<MapEntry<K, V>> prevNode = null;
    SinglyLinkedNode<MapEntry<K, V>> prevPrevNode = null;

    HashMapItr(DurableHashMapImpl<A, K, V> map) {
      this.map = map;
      currentBucketAddr = map.holder.get();
      maxBucketAddr = currentBucketAddr + MAX_OBJECT_SIZE * map.totalCapacity;
      nextValidBucket();
    }

    @Override
    public boolean hasNext() {
      return (null != currentNode);
    }

    public void nextValidBucket() {
      while ((null == currentNode) && (currentBucketAddr < maxBucketAddr)) {
        long handler = unsafe.getAddress(currentBucketAddr);
        if (0L != handler) {
          currentNode = SinglyLinkedNodeFactory.restore(allocator,
              listefproxies, listgftypes, handler, false, reclaimcontext);
          break;
        }
        currentBucketAddr += MAX_OBJECT_SIZE;
      }
    }

    @Override
    public MapEntry<K, V> next() {
      if (null != currentNode) {
        MapEntry<K, V> entry = currentNode.getItem();
        if (prevBucketAddr != currentBucketAddr) {
          prevPrevNode = null;
          prevBucketAddr = currentBucketAddr;
        } else {
          prevPrevNode = prevNode;
        }
        prevNode = currentNode;
        currentNode = currentNode.getNext();
        if (null == currentNode) {
          currentBucketAddr += MAX_OBJECT_SIZE;
          nextValidBucket();
        }
        return entry;
      } else {
        throw new NoSuchElementException();
      }
    }

    @Override
    public void remove() {
      if (null == prevPrevNode) {
        if (null == prevNode.getNext()) {
          unsafe.putAddress(prevBucketAddr, 0L);
          prevNode.destroy();
        } else {
          unsafe.putAddress(prevBucketAddr, prevNode.getNext().getHandler());
          prevNode.destroy();
          prevNode = null;
        }
      } else {
        prevPrevNode.setNext(prevNode.getNext(), false);
        prevNode.destroy();
        prevNode = prevPrevNode;
      }
      map.mapSize--;
    }
  }
}
