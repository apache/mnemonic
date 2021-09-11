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
import org.apache.mnemonic.GenericField;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.MemChunkHolder;
import org.apache.mnemonic.MemoryDurableEntity;
import org.apache.mnemonic.OutOfHybridMemory;
import org.apache.mnemonic.resgc.ReclaimContext;
import org.apache.mnemonic.RestorableAllocator;
import org.apache.mnemonic.RestoreDurableEntityError;
import org.apache.mnemonic.RetrieveDurableEntityError;
import org.apache.mnemonic.Utils;

import java.util.NoSuchElementException;
import java.util.Iterator;

@SuppressWarnings({"restriction", "unchecked"})
public class DurableArrayImpl<A extends RestorableAllocator<A>, E>
        extends DurableArray<E> implements MemoryDurableEntity<A> {

  private static final int MAX_OBJECT_SIZE = 8;
  private static long[][] fieldInfo;
  private Object[] genericField;
  @SuppressWarnings({"restriction", "UseOfSunClasses"})
  private sun.misc.Unsafe unsafe;
  private EntityFactoryProxy[] factoryProxy;
  private DurableType[] genericType;
  private volatile boolean autoReclaim;
  private volatile ReclaimContext reclaimcontext;
  private MemChunkHolder<A> holder;
  private A allocator;

  public DurableArrayImpl() {
    super(0);
  }

  public DurableArrayImpl(int size) {
    super(size);
  }

  /**
   * get item from the given index of array
   *
   * @return item from the given index of array
   */
  public E get(int index) {
    if (index >= arraySize) {
        throw new RetrieveDurableEntityError("Index greater than array size.");
    }
    if (null == genericField[index]) {
      EntityFactoryProxy proxy = null;
      DurableType gftype = null;
      if (null != factoryProxy) {
        proxy = factoryProxy[0];
      }
      if (null != genericType) {
        gftype = genericType[0];
      } else {
        throw new RetrieveDurableEntityError("No Generic Field Type Info.");
      }
      genericField[index] = new GenericField<A, E>(proxy, gftype, factoryProxy, genericType, allocator,
                                  unsafe, autoReclaim, reclaimcontext, holder.get() + index * MAX_OBJECT_SIZE);
    }
    if (null != genericField[index]) {
      return ((GenericField<A, E>)genericField[index]).get();
    } else {
      throw new RetrieveDurableEntityError("GenericField is null!");
    }
  }

  /**
   * set a value at a given index
   *
   * @param value
   *          the value to be set
   */
  public void set(int index, E value) {
    set(index, value, true);
  }

  /**
   * set a value at a given index
   *
   * @param value
   *          the value to be set
   *
   * @param destroy
   *          true if want to destroy exist one
   *
   */
  public void set(int index, E value, boolean destroy) {
    if (index >= arraySize) {
        throw new RetrieveDurableEntityError("Index greater than array size.");
    }
    if (null == genericField[index]) {
      EntityFactoryProxy proxy = null;
      DurableType gftype = null;
      if (null != factoryProxy) {
        proxy = factoryProxy[0];
      }
      if (null != genericType) {
        gftype = genericType[0];
      } else {
        throw new RetrieveDurableEntityError("No Generic Field Type Info.");
      }
      genericField[index] = new GenericField<A, E>(proxy, gftype, factoryProxy, genericType, allocator,
                                  unsafe, autoReclaim, reclaimcontext, holder.get() + index * MAX_OBJECT_SIZE);
    }
    if (null != genericField[index]) {
      ((GenericField<A, E>)genericField[index]).set(value, destroy);
    } else {
      throw new RetrieveDurableEntityError("GenericField is null!");
    }
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
    long startAddr = holder.get();
    long endAddr = startAddr + MAX_OBJECT_SIZE * arraySize;
    int index = 0;
    while (startAddr < endAddr) {
      if (null != get(index)) {
        genericField[index] = null;
      }
      index++;
      startAddr += MAX_OBJECT_SIZE;
    }
    holder.destroy();
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
    return allocator.getChunkHandler(holder);
  }

  @Override
  public void restoreDurableEntity(A allocator, EntityFactoryProxy[] factoryProxy,
             DurableType[] gType, long phandler, boolean autoReclaim, ReclaimContext rctx)
          throws RestoreDurableEntityError {
    initializeDurableEntity(allocator, factoryProxy, gType, autoReclaim, reclaimcontext);
    if (0L == phandler) {
      throw new RestoreDurableEntityError("Input handler is null on restoreDurableEntity.");
    }
    holder = allocator.retrieveChunk(phandler, autoReclaim, rctx);
    if (null == holder) {
      throw new RestoreDurableEntityError("Retrieve Entity Failure!");
    }
    arraySize = ((int)(holder.getSize() / MAX_OBJECT_SIZE));
    genericField = new Object[arraySize];
    initializeAfterRestore();
  }


  @Override
  public void initializeDurableEntity(A allocator, EntityFactoryProxy[] factoryProxy,
              DurableType[] gType, boolean autoReclaim, ReclaimContext rctx) {
    this.allocator = allocator;
    this.factoryProxy = factoryProxy;
    this.genericType = gType;
    this.autoReclaim = autoReclaim;
    this.reclaimcontext = rctx;
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
    this.holder = allocator.createChunk(MAX_OBJECT_SIZE * arraySize, autoReclaim, reclaimcontext);
    if (null == this.holder) {
      throw new OutOfHybridMemory("Create Durable Entity Error!");
    }
    genericField = new Object[arraySize];
    initializeAfterCreate();
  }

  @Override
  public Iterator<E> iterator() {
    return new ArrayItr(this);
  }

  private class ArrayItr implements Iterator<E> {

    protected DurableArray<E> array = null;
    int currentIndex = 0;

    ArrayItr(DurableArray<E> itr) {
      array = itr;
    }

    @Override
    public boolean hasNext() {
      return currentIndex < array.arraySize;
    }

    @Override
    public E next() {
      if (currentIndex >= array.arraySize) {
        throw new NoSuchElementException();
      }
      E item = get(currentIndex);
      currentIndex++;
      return item;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
