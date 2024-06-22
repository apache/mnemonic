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

import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.OutOfHybridMemory;
import org.apache.mnemonic.ParameterHolder;
import org.apache.mnemonic.RestorableAllocator;
import org.apache.mnemonic.RestoreDurableEntityError;
import org.apache.mnemonic.resgc.ReclaimContext;

/**
 * Factory class for creating and restoring instances of DurableSinglyLinkedList.
 */
public class DurableSinglyLinkedListFactory {

  /**
   * Creates a new DurableSinglyLinkedList using the provided ParameterHolder.
   * 
   * @param parameterholder the parameter holder with allocator and other parameters
   * @return a new instance of DurableSinglyLinkedList
   * @throws OutOfHybridMemory if there is insufficient hybrid memory
   */
  public static <A extends RestorableAllocator<A>, E> DurableSinglyLinkedList<E> create(
          ParameterHolder<A> parameterholder) throws OutOfHybridMemory {
    DurableSinglyLinkedListImpl<A, E> entity = new DurableSinglyLinkedListImpl<>();
    entity.setupGenericInfo(parameterholder.getEntityFactoryProxies(), parameterholder.getGenericTypes());
    entity.createDurableEntity(parameterholder.getAllocator(), parameterholder.getEntityFactoryProxies(),
            parameterholder.getGenericTypes(), parameterholder.getAutoReclaim(), null);
    return entity;
  }

  /**
   * Creates a new DurableSinglyLinkedList with the given allocator.
   * 
   * @param allocator the allocator to use
   * @return a new instance of DurableSinglyLinkedList
   * @throws OutOfHybridMemory if there is insufficient hybrid memory
   */
  public static <A extends RestorableAllocator<A>, E> DurableSinglyLinkedList<E> create(
          A allocator) throws OutOfHybridMemory {
    return create(allocator, false);
  }

  /**
   * Creates a new DurableSinglyLinkedList with the given allocator and auto reclaim setting.
   * 
   * @param allocator the allocator to use
   * @param autoreclaim whether to automatically reclaim memory
   * @return a new instance of DurableSinglyLinkedList
   * @throws OutOfHybridMemory if there is insufficient hybrid memory
   */
  public static <A extends RestorableAllocator<A>, E> DurableSinglyLinkedList<E> create(
          A allocator, boolean autoreclaim) throws OutOfHybridMemory {
    return create(allocator, null, null, autoreclaim, null);
  }

  /**
   * Creates a new DurableSinglyLinkedList with the given allocator, auto reclaim setting, and reclaim context.
   * 
   * @param allocator the allocator to use
   * @param autoreclaim whether to automatically reclaim memory
   * @param reclaimcontext the reclaim context
   * @return a new instance of DurableSinglyLinkedList
   * @throws OutOfHybridMemory if there is insufficient hybrid memory
   */
  public static <A extends RestorableAllocator<A>, E> DurableSinglyLinkedList<E> create(
          A allocator, boolean autoreclaim, ReclaimContext reclaimcontext) throws OutOfHybridMemory {
    return create(allocator, null, null, autoreclaim, reclaimcontext);
  }

  /**
   * Creates a new DurableSinglyLinkedList with the given allocator, entity factory proxies, and durable types.
   * 
   * @param allocator the allocator to use
   * @param factoryproxys the entity factory proxies
   * @param gfields the durable types
   * @param autoreclaim whether to automatically reclaim memory
   * @return a new instance of DurableSinglyLinkedList
   * @throws OutOfHybridMemory if there is insufficient hybrid memory
   */
  public static <A extends RestorableAllocator<A>, E> DurableSinglyLinkedList<E> create(
          A allocator, EntityFactoryProxy[] factoryproxys, DurableType[] gfields, boolean autoreclaim)
          throws OutOfHybridMemory {
    return create(allocator, factoryproxys, gfields, autoreclaim, null);
  }

  /**
   * Creates a new DurableSinglyLinkedList with the given parameters.
   * 
   * @param allocator the allocator to use
   * @param factoryproxys the entity factory proxies
   * @param gfields the durable types
   * @param autoreclaim whether to automatically reclaim memory
   * @param reclaimcontext the reclaim context
   * @return a new instance of DurableSinglyLinkedList
   * @throws OutOfHybridMemory if there is insufficient hybrid memory
   */
  public static <A extends RestorableAllocator<A>, E> DurableSinglyLinkedList<E> create(
          A allocator, EntityFactoryProxy[] factoryproxys, DurableType[] gfields,
          boolean autoreclaim, ReclaimContext reclaimcontext) throws OutOfHybridMemory {
    DurableSinglyLinkedListImpl<A, E> entity = new DurableSinglyLinkedListImpl<>();
    entity.setupGenericInfo(factoryproxys, gfields);
    entity.createDurableEntity(allocator, factoryproxys, gfields, autoreclaim, reclaimcontext);
    return entity;
  }

  /**
   * Restores a DurableSinglyLinkedList from the given allocator and persistent handler.
   * 
   * @param allocator the allocator to use
   * @param phandler the persistent handler
   * @return the restored instance of DurableSinglyLinkedList
   * @throws RestoreDurableEntityError if there is an error during restoration
   */
  public static <A extends RestorableAllocator<A>, E> DurableSinglyLinkedList<E> restore(
          A allocator, long phandler) throws RestoreDurableEntityError {
    return restore(allocator, phandler, false);
  }

  /**
   * Restores a DurableSinglyLinkedList using the provided ParameterHolder.
   * 
   * @param parameterholder the parameter holder with allocator and other parameters
   * @return the restored instance of DurableSinglyLinkedList
   * @throws RestoreDurableEntityError if there is an error during restoration
   */
  public static <A extends RestorableAllocator<A>, E> DurableSinglyLinkedList<E> restore(
          ParameterHolder<A> parameterholder) throws RestoreDurableEntityError {
    DurableSinglyLinkedListImpl<A, E> entity = new DurableSinglyLinkedListImpl<>();
    entity.setupGenericInfo(parameterholder.getEntityFactoryProxies(), parameterholder.getGenericTypes());
    entity.restoreDurableEntity(parameterholder.getAllocator(), parameterholder.getEntityFactoryProxies(),
            parameterholder.getGenericTypes(), parameterholder.getHandler(),
            parameterholder.getAutoReclaim(), null);
    return entity;
  }

  /**
   * Restores a DurableSinglyLinkedList from the given allocator, persistent handler, and auto reclaim setting.
   * 
   * @param allocator the allocator to use
   * @param phandler the persistent handler
   * @param autoreclaim whether to automatically reclaim memory
   * @return the restored instance of DurableSinglyLinkedList
   * @throws RestoreDurableEntityError if there is an error during restoration
   */
  public static <A extends RestorableAllocator<A>, E> DurableSinglyLinkedList<E> restore(
          A allocator, long phandler, boolean autoreclaim) throws RestoreDurableEntityError {
    return restore(allocator, null, null, phandler, autoreclaim, null);
  }

  /**
   * Restores a DurableSinglyLinkedList from the given allocator, persistent handler, auto reclaim setting, and reclaim context.
   * 
   * @param allocator the allocator to use
   * @param phandler the persistent handler
   * @param autoreclaim whether to automatically reclaim memory
   * @param reclaimcontext the reclaim context
   * @return the restored instance of DurableSinglyLinkedList
   * @throws RestoreDurableEntityError if there is an error during restoration
   */
  public static <A extends RestorableAllocator<A>, E> DurableSinglyLinkedList<E> restore(
          A allocator, long phandler, boolean autoreclaim, ReclaimContext reclaimcontext)
          throws RestoreDurableEntityError {
    return restore(allocator, null, null, phandler, autoreclaim, reclaimcontext);
  }

  /**
   * Restores a DurableSinglyLinkedList from the given parameters.
   * 
   * @param allocator the allocator to use
   * @param factoryproxys the entity factory proxies
   * @param gfields the durable types
   * @param phandler the persistent handler
   * @param autoreclaim whether to automatically reclaim memory
   * @return the restored instance of DurableSinglyLinkedList
   * @throws RestoreDurableEntityError if there is an error during restoration
   */
  public static <A extends RestorableAllocator<A>, E> DurableSinglyLinkedList<E> restore(
          A allocator, EntityFactoryProxy[] factoryproxys, DurableType[] gfields, long phandler, boolean autoreclaim)
          throws RestoreDurableEntityError {
    return restore(allocator, factoryproxys, gfields, phandler, autoreclaim, null);
  }

  /**
   * Restores a DurableSinglyLinkedList from the given parameters.
   * 
   * @param allocator the allocator to use
   * @param factoryproxys the entity factory proxies
   * @param gfields the durable types
   * @param phandler the persistent handler
   * @param autoreclaim whether to automatically reclaim memory
   * @param reclaimcontext the reclaim context
   * @return the restored instance of DurableSinglyLinkedList
   * @throws RestoreDurableEntityError if there is an error during restoration
   */
  public static <A extends RestorableAllocator<A>, E> DurableSinglyLinkedList<E> restore(
          A allocator, EntityFactoryProxy[] factoryproxys, DurableType[] gfields, long phandler,
          boolean autoreclaim, ReclaimContext reclaimcontext) throws RestoreDurableEntityError {
    DurableSinglyLinkedListImpl<A, E> entity = new DurableSinglyLinkedListImpl<>();
    entity.setupGenericInfo(factoryproxys, gfields);
    entity.restoreDurableEntity(allocator, factoryproxys, gfields, phandler, autoreclaim, reclaimcontext);
    return entity;
  }
}

