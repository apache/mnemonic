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
import org.apache.mnemonic.RestorableAllocator;
import org.apache.mnemonic.RestoreDurableEntityError;
import org.apache.mnemonic.resgc.ReclaimContext;

public class DurableTreeFactory {
  public static <A extends RestorableAllocator<A>, E extends Comparable<E>> DurableTree<E>
              create(A allocator) throws OutOfHybridMemory {
    return create(allocator, false);
  }

  public static <A extends RestorableAllocator<A>, E extends Comparable<E>> DurableTree<E>
              create(A allocator, boolean autoreclaim) throws OutOfHybridMemory {
    return create(allocator, null, null, autoreclaim, null);
  }

  public static <A extends RestorableAllocator<A>, E extends Comparable<E>> DurableTree<E>
              create(A allocator, boolean autoreclaim, ReclaimContext reclaimcontext)
          throws OutOfHybridMemory {
    return create(allocator, null, null, autoreclaim, reclaimcontext);
  }

  public static <A extends RestorableAllocator<A>, E extends Comparable<E>> DurableTree<E>
              create(A allocator, EntityFactoryProxy[] factoryproxys, DurableType[] gfields,
                   boolean autoreclaim) throws OutOfHybridMemory {
    return create(allocator, factoryproxys, gfields, autoreclaim, null);
  }

  public static <A extends RestorableAllocator<A>, E extends Comparable<E>> DurableTree<E>
              create(A allocator, EntityFactoryProxy[] factoryproxys, DurableType[] gfields,
                   boolean autoreclaim, ReclaimContext reclaimcontext) throws OutOfHybridMemory {
    DurableTreeImpl<A, E> entity = new DurableTreeImpl<A, E>();
    entity.setupGenericInfo(factoryproxys, gfields);
    entity.createDurableEntity(allocator, factoryproxys, gfields, autoreclaim, reclaimcontext);
    return entity;
  }

  public static <A extends RestorableAllocator<A>, E extends Comparable<E>> DurableTree<E>
              restore(A allocator, long phandler) throws RestoreDurableEntityError {
    return restore(allocator, phandler, false);
  }

  public static <A extends RestorableAllocator<A>, E extends Comparable<E>> DurableTree<E>
              restore(A allocator, long phandler, boolean autoreclaim) throws RestoreDurableEntityError {
    return restore(allocator, null, null, phandler, autoreclaim, null);
  }

  public static <A extends RestorableAllocator<A>, E extends Comparable<E>> DurableTree<E>
              restore(A allocator, long phandler, boolean autoreclaim, ReclaimContext reclaimcontext)
          throws RestoreDurableEntityError {
    return restore(allocator, null, null, phandler, autoreclaim, reclaimcontext);
  }

  public static <A extends RestorableAllocator<A>, E extends Comparable<E>> DurableTree<E>
              restore(A allocator, EntityFactoryProxy[] factoryproxys, DurableType[] gfields,
                   long phandler, boolean autoreclaim)
          throws RestoreDurableEntityError {
    return restore(allocator, factoryproxys, gfields, phandler, autoreclaim, null);
  }

  public static <A extends RestorableAllocator<A>, E extends Comparable<E>> DurableTree<E>
              restore(A allocator, EntityFactoryProxy[] factoryproxys, DurableType[] gfields,
                   long phandler, boolean autoreclaim, ReclaimContext reclaimcontext)
          throws RestoreDurableEntityError {
    DurableTreeImpl<A, E> entity = new DurableTreeImpl<A, E>();
    entity.setupGenericInfo(factoryproxys, gfields);
    entity.restoreDurableEntity(allocator, factoryproxys, gfields, phandler, autoreclaim, reclaimcontext);
    return entity;
  }
}
