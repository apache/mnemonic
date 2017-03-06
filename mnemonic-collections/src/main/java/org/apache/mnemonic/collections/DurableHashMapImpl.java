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

import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.MemChunkHolder;
import org.apache.mnemonic.MemoryDurableEntity;
import org.apache.mnemonic.OutOfHybridMemory;
import org.apache.mnemonic.RestorableAllocator;
import org.apache.mnemonic.RestoreDurableEntityError;
import org.apache.mnemonic.RetrieveDurableEntityError;
import org.apache.mnemonic.Utils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.ArrayUtils;
import sun.misc.Unsafe;

@SuppressWarnings("restriction")
public class DurableHashMapImpl<A extends RestorableAllocator<A>, K, V>
        extends DurableHashMap<K, V> implements MemoryDurableEntity<A> {

  private static final long DEFAULT_MAP_SIZE = 16;
  private static final float DEFAULT_MAP_LOAD_FACTOR = 0.75f;
  private static final long MAX_OBJECT_SIZE = 8;
  private static long[][] fieldInfo;
  private Unsafe unsafe;
  private EntityFactoryProxy[] factoryProxy;
  private EntityFactoryProxy[] listefproxies;
  private DurableType[] genericField;
  private DurableType[] listgftypes;
  private volatile boolean autoResize = true;
  private volatile boolean autoReclaim;
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
  public V put(K key, V value) {
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
  public V addEntry(K key, V value, long bucketAddr) {
    V retValue = null;
    long handler = unsafe.getAddress(bucketAddr);
    if (0L == handler) {
      DurableSinglyLinkedList<MapEntry<K, V>> head = DurableSinglyLinkedListFactory.create(allocator, 
          listefproxies, listgftypes, false);
      MapEntry<K, V> entry = MapEntryFactory.create(allocator, factoryProxy, genericField, false);
      entry.setKey(key, false);
      entry.setValue(value, false);
      head.setItem(entry, false);
      unsafe.putLong(bucketAddr, head.getHandler());
      mapSize++;
    } else {
      DurableSinglyLinkedList<MapEntry<K, V>> head = DurableSinglyLinkedListFactory.restore(allocator,
          listefproxies, listgftypes, handler, false);
      DurableSinglyLinkedList<MapEntry<K, V>> prev = head;
      boolean found = false;
      while (null != head) {
        MapEntry<K, V> mapEntry = head.getItem();
        K entryKey = mapEntry.getKey();
        if (entryKey == key || entryKey.equals(key)) {
          retValue = mapEntry.getValue();
          mapEntry.setValue(value, false);
          found = true;
          break;
        }
        prev = head;
        head = head.getNext();
      }
      if (true != found) {
        DurableSinglyLinkedList<MapEntry<K, V>> newNode = DurableSinglyLinkedListFactory.create(allocator, 
            listefproxies, listgftypes, false);
        MapEntry<K, V> entry = MapEntryFactory.create(allocator, factoryProxy, genericField, false);
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
  public V getEntry(K key, long bucketAddr) {
    V retValue = null;
    long handler = unsafe.getAddress(bucketAddr);
    if (0L != handler) {
      DurableSinglyLinkedList<MapEntry<K, V>> head = DurableSinglyLinkedListFactory.restore(allocator,
          listefproxies, listgftypes, handler, false);
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
  public V removeEntry(K key, long bucketAddr) {
    V retValue = null;
    long handler = unsafe.getAddress(bucketAddr);
    if (0L != handler) {
      DurableSinglyLinkedList<MapEntry<K, V>> head = DurableSinglyLinkedListFactory.restore(allocator,
          listefproxies, listgftypes, handler, false);
      DurableSinglyLinkedList<MapEntry<K, V>> prev = null;
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
            head.setNext(null, false);
            head.destroy(); // #TODO: better way to delete one node
          }
        } else {
          prev.setNext(head.getNext(), false);
          head.setNext(null, false);
          head.destroy(); // #TODO: better way to delete one node
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
    totalCapacity = newCapacity;
    threshold = (long) (totalCapacity * DEFAULT_MAP_LOAD_FACTOR);
    holder = allocator.createChunk(MAX_OBJECT_SIZE * totalCapacity, autoReclaim);
    unsafe.putLong(chunkAddr.get(), allocator.getChunkHandler(holder));
    while (bucketAddr < maxbucketAddr) {
      long handler = unsafe.getAddress(bucketAddr);
      if (0L != handler) {
        DurableSinglyLinkedList<MapEntry<K, V>> head = DurableSinglyLinkedListFactory.restore(allocator,
            listefproxies, listgftypes, handler, false);
        DurableSinglyLinkedList<MapEntry<K, V>> curr = head;
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
  public void transfer(DurableSinglyLinkedList<MapEntry<K, V>> elem) {
    int hash = hash(elem.getItem().getKey().hashCode());
    long bucketIndex = getBucketIndex(hash);
    long bucketAddr = holder.get() + MAX_OBJECT_SIZE * bucketIndex;
    long handler = unsafe.getAddress(bucketAddr);
    if (0L != handler) {
      DurableSinglyLinkedList<MapEntry<K, V>> head = DurableSinglyLinkedListFactory.restore(allocator,
          listefproxies, listgftypes, handler, false);
      elem.setNext(head, false);
    } else {
      elem.setNext(null, false);
    }
    unsafe.putLong(bucketAddr, elem.getHandler());
  }

  /**
   * Recomputes the size of the map during restore without persistence
   * 
   */
  public long recomputeMapSize() {
    long size = 0;
    long bucketAddr = holder.get();
    long maxbucketAddr = bucketAddr + MAX_OBJECT_SIZE * totalCapacity;
    while (bucketAddr < maxbucketAddr) {
      long handler = unsafe.getAddress(bucketAddr);
      if (0L != handler) {
        DurableSinglyLinkedList<MapEntry<K, V>> head = DurableSinglyLinkedListFactory.restore(allocator,
            listefproxies, listgftypes, handler, false);
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

  @Override
  public long[][] getNativeFieldInfo() {
    return fieldInfo;
  }

  @Override
  public void destroy() throws RetrieveDurableEntityError {
    long bucketAddr = holder.get();
    long maxbucketAddr = bucketAddr + MAX_OBJECT_SIZE * totalCapacity;
    while (bucketAddr < maxbucketAddr) {
      long handler = unsafe.getAddress(bucketAddr);
      if (0L != handler) {
        DurableSinglyLinkedList<MapEntry<K, V>> head = DurableSinglyLinkedListFactory.restore(allocator,
            listefproxies, listgftypes, handler, false);
        head.destroy();
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
    holder.registerAutoReclaim();
    autoReclaim = true;
  }

  @Override
  public long getHandler() {
    return allocator.getChunkHandler(chunkAddr);
  }

  @Override
  public void restoreDurableEntity(A allocator, EntityFactoryProxy[] factoryProxy, 
             DurableType[] gField, long phandler, boolean autoreclaim) throws RestoreDurableEntityError {
    initializeDurableEntity(allocator, factoryProxy, gField, autoreclaim);
    if (0L == phandler) {
      throw new RestoreDurableEntityError("Input handler is null on restoreDurableEntity.");
    }
    chunkAddr = allocator.retrieveChunk(phandler, autoreclaim);
    long chunkHandler = unsafe.getLong(chunkAddr.get());
    holder = allocator.retrieveChunk(chunkHandler, autoReclaim);
    if (null == holder || null == chunkAddr) {
      throw new RestoreDurableEntityError("Retrieve Entity Failure!");
    }
    setCapacityHint(holder.getSize() / MAX_OBJECT_SIZE);
    mapSize = recomputeMapSize();
    initializeAfterRestore();
  }


  @Override
  public void initializeDurableEntity(A allocator, EntityFactoryProxy[] factoryProxy, 
              DurableType[] gField, boolean autoReclaim) {
    this.allocator = allocator;
    this.factoryProxy = factoryProxy;
    this.genericField = gField;
    this.autoReclaim = autoReclaim;
    DurableType gftypes[] = {DurableType.DURABLE};
    this.listgftypes = ArrayUtils.addAll(gftypes, genericField);
    EntityFactoryProxy efproxies[] = {new EntityFactoryProxy() {
      @Override
      public <A extends RestorableAllocator<A>> MapEntry<K, V> restore(
          A allocator, EntityFactoryProxy[] factoryproxys,
          DurableType[] gfields, long phandler, boolean autoreclaim) {
          Pair<DurableType[], EntityFactoryProxy[]> dpt = Utils.shiftDurableParams(gfields, factoryproxys, 1);
        return MapEntryFactory.restore(allocator, dpt.getRight(), dpt.getLeft(), phandler, autoreclaim);
          }
      @Override
      public <A extends RestorableAllocator<A>> MapEntry<K, V> create(
          A allocator, EntityFactoryProxy[] factoryproxys,
          DurableType[] gfields, boolean autoreclaim) {
          Pair<DurableType[], EntityFactoryProxy[]> dpt = Utils.shiftDurableParams(gfields, factoryproxys, 1);
        return MapEntryFactory.create(allocator, dpt.getRight(), dpt.getLeft(), autoreclaim);
          }
    }
    };
    this.listefproxies = ArrayUtils.addAll(efproxies, factoryProxy);
    try {
      this.unsafe = Utils.getUnsafe();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void createDurableEntity(A allocator, EntityFactoryProxy[] factoryProxy, 
              DurableType[] gField, boolean autoreclaim) throws OutOfHybridMemory {
    initializeDurableEntity(allocator, factoryProxy, gField, autoreclaim);
    this.holder = allocator.createChunk(MAX_OBJECT_SIZE * totalCapacity, autoreclaim);
    this.chunkAddr = allocator.createChunk(MAX_OBJECT_SIZE, autoreclaim);
    unsafe.putLong(chunkAddr.get(), allocator.getChunkHandler(holder));
    if (null == this.holder || null == this.chunkAddr) {
      throw new OutOfHybridMemory("Create Durable Entity Error!");
    }
    initializeAfterCreate();
  }
}
 
