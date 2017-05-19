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
import org.apache.mnemonic.MemoryDurableEntity;
import org.apache.mnemonic.OutOfHybridMemory;
import org.apache.mnemonic.RestorableAllocator;
import org.apache.mnemonic.RestoreDurableEntityError;
import org.apache.mnemonic.RetrieveDurableEntityError;
import org.apache.mnemonic.Utils;

import sun.misc.Unsafe;
import java.util.Iterator;

@SuppressWarnings("restriction")
public class DurableHashSetImpl<A extends RestorableAllocator<A>, E>
        extends DurableHashSet<E> implements MemoryDurableEntity<A> {

  private static long[][] fieldInfo;
  private Unsafe unsafe;
  private EntityFactoryProxy[] factoryProxy;
  private DurableType[] genericType;
  private volatile boolean autoReclaim;
  private DurableHashMap<E, Object> map;
  private A allocator;
  private long initialCapacity;

  public void setCapacityHint(long capacity) {
    initialCapacity = capacity;
  }

  @Override
  public long getSize() {
    return map.getSize();
  }
  /**
   * adds a specific element to the set
   *
   * @return true if set did not already contain the element
   */
  public boolean add(E item) {
    //add logic for adding item
    return true;
  }

  /**
   * removes a specific element from the set
   *
   * @return true if set contained the element
   */
  public boolean remove(E value) {
    //add logic for removing item
    return true;
  }

  /**
   * checks if set contains the specified element
   *
   * @return true if set contains the element
   */
  public boolean contains(E item) {
    //add logic to check if set contains the item
    return true;
  }

  @Override
  public boolean autoReclaim() {
    return autoReclaim;
  }

  /**
   * sync. this object
   */
  @Override
  public void sync() {

  }

  /**
   * Make any cached changes to this object persistent.
   */
  @Override
  public void persist() {

  }

  /**
   * flush processors cache for this object
   */
  @Override
  public void flush() {

  }

  @Override
  public long[][] getNativeFieldInfo() {
    return fieldInfo;
  }

  @Override
  public void destroy() throws RetrieveDurableEntityError {
    map.destroy();
  }

  @Override
  public void cancelAutoReclaim() {
    map.cancelAutoReclaim();
    autoReclaim = false;
  }

  @Override
  public void registerAutoReclaim() {
    map.registerAutoReclaim();
    autoReclaim = true;
  }

  @Override
  public long getHandler() {
    return map.getHandler();
  }

  @Override
  public void restoreDurableEntity(A allocator, EntityFactoryProxy[] factoryProxy,
             DurableType[] gType, long phandler, boolean autoReclaim) throws RestoreDurableEntityError {
    initializeDurableEntity(allocator, factoryProxy, gType, autoReclaim);
    if (0L == phandler) {
      throw new RestoreDurableEntityError("Input handler is null on restoreDurableEntity.");
    }
    map = DurableHashMapFactory.restore(allocator, factoryProxy, gType, phandler, autoReclaim);
    if (null == map) {
      throw new RestoreDurableEntityError("Retrieve Entity Failure!");
    }
    initializeAfterRestore();
  }


  @Override
  public void initializeDurableEntity(A allocator, EntityFactoryProxy[] factoryProxy,
              DurableType[] gType, boolean autoReclaim) {
    this.allocator = allocator;
    this.factoryProxy = factoryProxy;
    this.genericType = gType;
    this.autoReclaim = autoReclaim;
    try {
      this.unsafe = Utils.getUnsafe();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void createDurableEntity(A allocator, EntityFactoryProxy[] factoryProxy,
              DurableType[] gType, boolean autoReclaim) throws OutOfHybridMemory {
    initializeDurableEntity(allocator, factoryProxy, gType, autoReclaim);
    map = DurableHashMapFactory.create(allocator, factoryProxy, gType, initialCapacity, autoReclaim); 
    initializeAfterCreate();
  }

  @Override
  public Iterator<E> iterator() {
    return null;
  }
}
