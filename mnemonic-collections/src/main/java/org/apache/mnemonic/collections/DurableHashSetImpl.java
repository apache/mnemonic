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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.mnemonic.resgc.ReclaimContext;
import java.util.Iterator;

@SuppressWarnings("restriction")
public class DurableHashSetImpl<A extends RestorableAllocator<A>, E>
        extends DurableHashSet<E> implements MemoryDurableEntity<A> {

  private static long[][] fieldInfo;
  @SuppressWarnings({"restriction", "UseOfSunClasses"})
  private sun.misc.Unsafe unsafe;
  private EntityFactoryProxy[] factoryProxy;
  private DurableType[] genericType;
  private volatile boolean autoReclaim;
  private volatile ReclaimContext reclaimcontext;
  private DurableHashMap<E, Boolean> map;
  private A allocator;
  private long initialCapacity;
  private static final Boolean DEFAULT = Boolean.TRUE;

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
   * @param item
   *          the item to be added
   *
   * @return true if set did not already contain the element
   */
  public boolean add(E item) {
    return map.put(item, DEFAULT) == null;
  }

  /**
   * removes a specific element from the set
   *
   * @param item
   *          the item to be removed
   *
   * @return true if set contained the element
   */
  public boolean remove(E item) {
    return map.remove(item) == DEFAULT;
  }

  /**
   * checks if set contains the specified element
   *
   * @param item
   *          the item to be searched
   *
   * @return true if set contains the element
   */
  public boolean contains(E item) {
    return map.get(item) != null;
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
    map.destroy();
  }

  @Override
  public void cancelAutoReclaim() {
    map.cancelAutoReclaim();
    autoReclaim = false;
  }

  @Override
  public void registerAutoReclaim() {
    this.registerAutoReclaim(reclaimcontext);
  }

  @Override
  public void registerAutoReclaim(ReclaimContext rctx) {
    map.registerAutoReclaim(rctx);
    autoReclaim = true;
    reclaimcontext = rctx;
  }

  @Override
  public long getHandler() {
    return map.getHandler();
  }

  @Override
  public void restoreDurableEntity(A allocator, EntityFactoryProxy[] factoryProxy,
             DurableType[] gType, long phandler, boolean autoReclaim, ReclaimContext rctx)
          throws RestoreDurableEntityError {
    initializeDurableEntity(allocator, factoryProxy, gType, autoReclaim, rctx);
    if (0L == phandler) {
      throw new RestoreDurableEntityError("Input handler is null on restoreDurableEntity.");
    }
    map = DurableHashMapFactory.restore(allocator, this.factoryProxy, this.genericType, phandler,
            autoReclaim, reclaimcontext);
    if (null == map) {
      throw new RestoreDurableEntityError("Retrieve Entity Failure!");
    }
    initializeAfterRestore();
  }


  @Override
  public void initializeDurableEntity(A allocator, EntityFactoryProxy[] factoryProxy,
              DurableType[] gType, boolean autoReclaim, ReclaimContext rctx) {
    this.allocator = allocator;
    this.autoReclaim = autoReclaim;
    DurableType gftypes[] = {DurableType.BOOLEAN};
    this.genericType = ArrayUtils.addAll(gType, gftypes);
    this.factoryProxy = ArrayUtils.addAll(factoryProxy, (EntityFactoryProxy[])null);
    try {
      this.unsafe = Utils.getUnsafe();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void createDurableEntity(A allocator, EntityFactoryProxy[] factoryProxy,
              DurableType[] gType, boolean autoReclaim, ReclaimContext rctx) throws OutOfHybridMemory {
    initializeDurableEntity(allocator, factoryProxy, gType, autoReclaim, rctx);
    map = DurableHashMapFactory.create(allocator, this.factoryProxy, this.genericType, initialCapacity,
            autoReclaim, reclaimcontext);
    initializeAfterCreate();
  }

  @Override
  public Iterator<E> iterator() {
    return new HashSetItr(map.iterator());
  }

  private class HashSetItr implements Iterator<E> {

    Iterator<MapEntry<E, Boolean>> mapItr;

    HashSetItr(Iterator<MapEntry<E, Boolean>> itr) {
      this.mapItr = itr;
    }

    @Override
    public boolean hasNext() {
      return mapItr.hasNext();
    }

    @Override
    public E next() {
      return mapItr.next().getKey();
    }

    @Override
    public void remove() {
      mapItr.remove();
    }


  }
}
