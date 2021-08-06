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

import org.apache.mnemonic.DurableChunk;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.MemoryDurableEntity;
import org.apache.mnemonic.OutOfHybridMemory;
import org.apache.mnemonic.RestorableAllocator;
import org.apache.mnemonic.RestoreDurableEntityError;
import org.apache.mnemonic.RetrieveDurableEntityError;
import org.apache.mnemonic.Utils;
import org.apache.mnemonic.resgc.ReclaimContext;

@SuppressWarnings("restriction")
public class DurableSinglyLinkedListImpl<A extends RestorableAllocator<A>, E>
        extends DurableSinglyLinkedList<E> implements MemoryDurableEntity<A> {
  private static long[][] fieldInfo;
  @SuppressWarnings({"restriction", "UseOfSunClasses"})
  private sun.misc.Unsafe unsafe;
  private EntityFactoryProxy[] factoryProxy;
  private DurableType[] genericType;
  private volatile boolean autoReclaim;
  private volatile ReclaimContext reclaimcontext;
  private A allocator;
  private DurableChunk<A> holder;

  @Override
  public SinglyLinkedNode<E> getNode(long handler) {
    if (0L != handler) {
      return SinglyLinkedNodeFactory.restore(
              allocator, m_node_efproxies, m_node_gftypes, handler, autoReclaim, reclaimcontext);
    }
    return null;
  }


  @Override
  public boolean forwardNode() {
    boolean ret;
    if (null == m_cur_node) {
      if (0L != pheadhandler) {
        m_cur_node = SinglyLinkedNodeFactory.restore(
                allocator, m_node_efproxies, m_node_gftypes, pheadhandler, autoReclaim, reclaimcontext);
      }
    } else {
      m_cur_node = m_cur_node.getNext();
    }
    ret = null != m_cur_node;
    return ret;
  }

  @Override
  public boolean addNode(SinglyLinkedNode<E> newnode) {
    SinglyLinkedNode<E> headnode, nextnode;
    if (null != newnode) {
      if (null == m_cur_node) {
        if (0L != pheadhandler) {
          headnode = SinglyLinkedNodeFactory.restore(
                  allocator, m_node_efproxies, m_node_gftypes, pheadhandler, autoReclaim, reclaimcontext);
          nextnode = headnode.getNext();
          newnode.setNext(nextnode, autoReclaim);
          headnode.setNext(newnode, autoReclaim);
        } else {
          pheadhandler = newnode.getHandler();
        }
      } else {
        nextnode = m_cur_node.getNext();
        newnode.setNext(nextnode, autoReclaim);
        m_cur_node.setNext(newnode, autoReclaim);
      }
    }
    return true;
  }

  @Override
  public boolean add(E item) {
    boolean ret = false;
    SinglyLinkedNode<E> newnode;
    if (null != item) {
      newnode = SinglyLinkedNodeFactory.create(
              allocator, m_node_efproxies, m_node_gftypes, autoReclaim, reclaimcontext);
      newnode.setItem(item, autoReclaim);
      ret = addNode(newnode);
    }
    return ret;
  }

  @Override
  public SinglyLinkedNode<E> createNode() {
    return SinglyLinkedNodeFactory.create(
            allocator, m_node_efproxies, m_node_gftypes, autoReclaim, reclaimcontext);
  }

  @Override
  public boolean autoReclaim() {
    return autoReclaim;
  }

  @Override
  public void syncToLocal() {
    holder.syncToLocal();
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
  public void syncToVolatileMemory() {
    holder.syncToVolatileMemory();
  }

  @Override
  public void destroy() throws RetrieveDurableEntityError {
    getNode(pheadhandler).destroy();
  }

  @Override
  public void syncToNonVolatileMemory() {
    holder.syncToNonVolatileMemory();
  }

  @Override
  public void cancelAutoReclaim() {
    autoReclaim = false;
    if (null != getCurrentNode()) {
      getCurrentNode().cancelAutoReclaim();
    }
  }

  @Override
  public void registerAutoReclaim() {
    this.registerAutoReclaim(reclaimcontext);
  }

  @Override
  public void registerAutoReclaim(ReclaimContext rctx) {
    autoReclaim = true;
    reclaimcontext = rctx;
    if (null != getCurrentNode()) {
      getCurrentNode().registerAutoReclaim(rctx);
    }
  }

  @Override
  public long getHandler() {
    return pheadhandler;
  }

  @Override
  public void restoreDurableEntity(A allocator, EntityFactoryProxy[] factoryProxy,
                                   DurableType[] gType, long pheadhandler, boolean autoReclaim, ReclaimContext rctx)
          throws RestoreDurableEntityError {
    this.pheadhandler = pheadhandler;
    initializeDurableEntity(allocator, factoryProxy, gType, autoReclaim, rctx);
    if (0L == pheadhandler) {
      throw new RestoreDurableEntityError("Input handler is null on restoreDurableEntity.");
    }
    initializeAfterRestore();
  }


  @Override
  public void initializeDurableEntity(A allocator, EntityFactoryProxy[] factoryProxy,
                                      DurableType[] gType, boolean autoReclaim, ReclaimContext rctx) {
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
                                  DurableType[] gType, boolean autoReclaim, ReclaimContext rctx)
                                  throws OutOfHybridMemory {
    initializeDurableEntity(allocator, factoryProxy, gType, autoReclaim, rctx);
    initializeAfterCreate();
  }
}
