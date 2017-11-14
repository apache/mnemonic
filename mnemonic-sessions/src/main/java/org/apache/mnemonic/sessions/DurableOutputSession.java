/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mnemonic.sessions;

import org.apache.mnemonic.ConfigurationException;
import org.apache.mnemonic.Durable;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.OutOfHybridMemory;
import org.apache.mnemonic.RestorableAllocator;
import org.apache.mnemonic.collections.DurableSinglyLinkedList;
import org.apache.mnemonic.collections.DurableSinglyLinkedListFactory;
import org.apache.mnemonic.collections.SinglyLinkedNode;
import org.apache.mnemonic.collections.SinglyLinkedNodeFactory;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public abstract class DurableOutputSession<V, A extends RestorableAllocator<A>>
    implements OutputSession<V, A> {

  private long poolSize;
  private String serviceName;
  private DurableType[] durableTypes;
  private EntityFactoryProxy[] entityFactoryProxies;
  private long slotKeyId;

  protected Map<V, SinglyLinkedNode<V>> m_recordmap =
      new HashMap<V, SinglyLinkedNode<V>>();
  protected boolean m_newpool;
  protected long m_poolidx = 0L;
  protected Pair<DurableType[], EntityFactoryProxy[]> m_recparmpair;
  protected DurableSinglyLinkedList<V> m_list;
  protected A m_act;

  /**
   * Initialize the next pool, must be called before use
   * and after configuration
   * @return true if success
   */
  protected abstract boolean initNextPool();

  @Override
  public A getAllocator() {
    return m_act;
  }

  public void setAllocator(A alloc) {
    m_act = alloc;
  }

  @Override
  public long getHandler() {
    long ret = 0L;
    if (null != m_list) {
      m_list.getHandler();
    }
    return ret;
  }

  @SuppressWarnings("unchecked")
  protected V createDurableObjectRecord(long size) {
    V ret = null;
    switch (getDurableTypes()[0]) {
    case DURABLE:
      ret = (V) getEntityFactoryProxies()[0].create(m_act,
          m_recparmpair.getRight(), m_recparmpair.getLeft(), false);
    case BUFFER:
      if (size > 0) {
        ret = (V)m_act.createBuffer(size);
        if (null == ret) {
          throw new OutOfHybridMemory("Allocate a buffer failed");
        }
      }
      break;
    case CHUNK:
      if (size > 0) {
        ret = (V)m_act.createChunk(size);
        if (null == ret) {
          throw new OutOfHybridMemory("Allocate a chunk failed");
        }
      }
      break;
    default:
      break;
    }
    return ret;
  }

  @Override
  public V newDurableObjectRecord() {
    return newDurableObjectRecord(-1L);
  }

  /**
   * create a durable object record
   *
   * @param size
   *        size of buffer or chunk
   *
   * @return null if size not greater than 0 for buffer/chunk type
   *        throw OutOfHybridMemory if out of memory
   */
  @Override
  public V newDurableObjectRecord(long size) {
    V ret = null;
    SinglyLinkedNode<V> nv = null;
    if (null == m_act) {
      if (!initNextPool()) {
        throw new ConfigurationException("init next pool failure");
      }
    }
    try {
      nv = createDurableNode();
      ret = createDurableObjectRecord(size);
    } catch (OutOfHybridMemory e) {
      if (nv != null) {
        nv.destroy();
        nv = null;
      }
      if (ret != null) {
        ((Durable) ret).destroy();
        ret = null;
      }
      if (initNextPool()) {
        try { /* retry */
          nv = createDurableNode();
          ret = createDurableObjectRecord(size);
        } catch (OutOfHybridMemory ee) {
          if (nv != null) {
            nv.destroy();
            nv = null;
          }
          if (ret != null) {
            ((Durable) ret).destroy();
            ret = null;
          }
        }
      } else {
        throw new ConfigurationException("try to init new next pool failure");
      }
    }
    if (null != ret && null != nv) {
      m_recordmap.put(ret, nv);
    } else {
      if (null != nv) {
        nv.destroy();
        nv = null;
      }
      if (ret != null) {
        ((Durable) ret).destroy();
        ret = null;
      }
    }
    return ret;
  }

  protected SinglyLinkedNode<V> createDurableNode() {
    SinglyLinkedNode<V> ret = null;
    ret = SinglyLinkedNodeFactory.create(m_act, getEntityFactoryProxies(), getDurableTypes(), false);
    return ret;
  }

  @Override
  public void post(V v) {
    SinglyLinkedNode<V> nv = null;
    if (null == m_act) {
      if (!initNextPool()) {
        throw new ConfigurationException("init next pool failure in post");
      }
    }
    if (null == v) {
      return;
    }
    switch (getDurableTypes()[0]) {
    case DURABLE:
    case BUFFER:
    case CHUNK:
      if (m_recordmap.containsKey(v)) {
        nv = m_recordmap.remove(v);
      } else {
        throw new RuntimeException("The record hasn't been created by newDurableObjectRecord(...) "
              + "Please make sure the overrides of hashCode() and/or equals() are appropriate.");
      }
      break;
    default:
      try {
        nv = createDurableNode();
      } catch (OutOfHybridMemory e) {
        if (initNextPool()) {
          nv = createDurableNode();
        }
      }
      break;
    }
    assert null != nv;
    nv.setItem(v, false);
    if (m_newpool) {
      m_act.setHandler(getSlotKeyId(), nv.getHandler());
      m_newpool = false;
      m_list = DurableSinglyLinkedListFactory.create(
              m_act, getEntityFactoryProxies(), getDurableTypes(), false);
    }
    m_list.addNode(nv);
    m_list.forwardNode();
  }

  @Override
  public void destroyPendingRecord(V k) {
    if (m_recordmap.containsKey(k)) {
      m_recordmap.get(k).destroy();
      ((Durable) k).destroy();
    }
  }

  @Override
  public void destroyAllPendingRecords() {
    for (V k : m_recordmap.keySet()) {
      destroyPendingRecord(k);
    }
  }

  @Override
  public void close() {
    if (null != m_act) {
      destroyAllPendingRecords();
      m_act.close();
      m_act = null;
    }
  }

  public long getSlotKeyId() {
    return slotKeyId;
  }

  public void setSlotKeyId(long slotKeyId) {
    this.slotKeyId = slotKeyId;
  }

  public EntityFactoryProxy[] getEntityFactoryProxies() {
    return entityFactoryProxies;
  }

  public void setEntityFactoryProxies(EntityFactoryProxy[] entityFactoryProxies) {
    this.entityFactoryProxies = entityFactoryProxies;
  }

  public DurableType[] getDurableTypes() {
    return durableTypes;
  }

  public void setDurableTypes(DurableType[] durableTypes) {
    this.durableTypes = durableTypes;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public long getPoolSize() {
    return poolSize;
  }

  public void setPoolSize(long poolSize) {
    this.poolSize = poolSize;
  }

}
