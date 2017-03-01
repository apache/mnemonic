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

package org.apache.mnemonic.hadoop;

import org.apache.mnemonic.ConfigurationException;
import org.apache.mnemonic.Durable;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.NonVolatileMemAllocator;
import org.apache.mnemonic.OutOfHybridMemory;
import org.apache.mnemonic.Utils;
import org.apache.mnemonic.collections.DurableSinglyLinkedList;
import org.apache.mnemonic.collections.DurableSinglyLinkedListFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class MneDurableOutputSession<V>
    implements MneOutputSession<V>, MneDurableComputable<NonVolatileMemAllocator> {

  private long poolSize;
  private TaskAttemptContext taskAttemptContext;
  private String serviceName;
  private DurableType[] durableTypes;
  private EntityFactoryProxy[] entityFactoryProxies;
  private long slotKeyId;
  private String baseOutputName;
  private Path outputPath;

  protected Map<V, DurableSinglyLinkedList<V>> m_recordmap;
  protected boolean m_newpool;
  protected long m_poolidx = 0L;
  protected Pair<DurableType[], EntityFactoryProxy[]> m_recparmpair;
  protected DurableSinglyLinkedList<V> m_listnode;
  protected NonVolatileMemAllocator m_act;
  protected Iterator<V> m_iter;

  public MneDurableOutputSession(TaskAttemptContext taskAttemptContext) {
    setTaskAttemptContext(taskAttemptContext);
    m_recordmap = new HashMap<V, DurableSinglyLinkedList<V>>();
  }

  public void validateConfig() {
    if (getDurableTypes().length < 1) {
      throw new ConfigurationException("The durable type of record parameters does not exist");
    } else {
      if (DurableType.DURABLE == getDurableTypes()[0]
          && getEntityFactoryProxies().length < 1) { /* T.B.D. BUFFER & CHUNK */
        throw new ConfigurationException("The durable entity proxy of record parameters does not exist");
      }
    }
  }

  @Override
  public void readConfig(String prefix) {
    if (getTaskAttemptContext() == null) {
      throw new ConfigurationException("taskAttemptContext has not yet been set");
    }
    Configuration conf = getTaskAttemptContext().getConfiguration();
    setServiceName(MneConfigHelper.getMemServiceName(conf, MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX));
    setDurableTypes(MneConfigHelper.getDurableTypes(conf, MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX));
    setEntityFactoryProxies(Utils.instantiateEntityFactoryProxies(
        MneConfigHelper.getEntityFactoryProxies(conf, MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX)));
    m_recparmpair = Utils.shiftDurableParams(getDurableTypes(), getEntityFactoryProxies(), 1);
    setSlotKeyId(MneConfigHelper.getSlotKeyId(conf, MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX));
    setPoolSize(MneConfigHelper.getMemPoolSize(conf, MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX));
    setBaseOutputName(MneConfigHelper.getBaseOutputName(conf, null));
    validateConfig();
  }

  protected Path genNextPoolPath() {
    Path ret = new Path(FileOutputFormat.getOutputPath(getTaskAttemptContext()),
        FileOutputFormat.getUniqueFile(getTaskAttemptContext(),
            String.format("%s-%05d", getBaseOutputName(), ++m_poolidx), MneConfigHelper.DEFAULT_FILE_EXTENSION));
    return ret;
  }

  @Override
  public void initNextPool() {
    if (m_act != null) {
      m_act.close();
    }
    setOutputPath(genNextPoolPath());
    m_act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService(getServiceName()), getPoolSize(),
        getOutputPath().toString(), true);
    m_newpool = true;
  }

  @Override
  public NonVolatileMemAllocator getAllocator() {
    return m_act;
  }

  @Override
  public long getHandler() {
    long ret = 0L;
    if (null != m_listnode) {
      m_listnode.getHandler();
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
  public V newDurableObjectRecord(long size) {
    V ret = null;
    DurableSinglyLinkedList<V> nv = null;
    try {
      nv = createDurableNode();
      ret = createDurableObjectRecord(size);
    } catch (OutOfHybridMemory e) {
      if (nv != null) {
        nv.destroy();
      }
      if (ret != null) {
        ((Durable) ret).destroy();
      }
      initNextPool();
      try { /* retry */
        nv = createDurableNode();
        ret = createDurableObjectRecord(size);
      } catch (OutOfHybridMemory ee) {
        if (nv != null) {
          nv.destroy();
        }
        if (ret != null) {
          ((Durable) ret).destroy();
        }
      }
    }
    if (null != ret) {
      m_recordmap.put(ret, nv);
    } else {
      if (null != nv) {
        nv.destroy();
      }
    }
    return ret;
  }

  protected DurableSinglyLinkedList<V> createDurableNode() {
    DurableSinglyLinkedList<V> ret = null;
    ret = DurableSinglyLinkedListFactory.create(m_act, getEntityFactoryProxies(), getDurableTypes(), false);
    return ret;
  }

  @Override
  public void post(V v) {
    DurableSinglyLinkedList<V> nv = null;
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
        throw new RuntimeException("The record hasn't been created by newDurableObjectRecord()");
      }
      break;
    default:
      try {
        nv = createDurableNode();
      } catch (OutOfHybridMemory e) {
        initNextPool();
        nv = createDurableNode();
      }
      break;
    }
    assert null != nv;
    nv.setItem(v, false);
    if (m_newpool) {
      m_act.setHandler(getSlotKeyId(), nv.getHandler());
      m_newpool = false;
    } else {
      m_listnode.setNext(nv, false);
    }
    m_listnode = nv;
  }

  public void destroyPendingRecord(V k) {
    if (m_recordmap.containsKey(k)) {
      m_recordmap.get(k).destroy();
      ((Durable) k).destroy();
    }
  }

  public void destroyAllPendingRecords() {
    for (V k : m_recordmap.keySet()) {
      destroyPendingRecord(k);
    }
  }

  @Override
  public void close() {
    destroyAllPendingRecords();
    m_act.close();
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

  public Path getOutputPath() {
    return outputPath;
  }

  public void setOutputPath(Path outputPath) {
    this.outputPath = outputPath;
  }

  public void setPoolSize(long poolSize) {
    this.poolSize = poolSize;
  }

  public TaskAttemptContext getTaskAttemptContext() {
    return taskAttemptContext;
  }

  public void setTaskAttemptContext(TaskAttemptContext taskAttemptContext) {
    this.taskAttemptContext = taskAttemptContext;
  }

  public String getBaseOutputName() {
    return baseOutputName;
  }

  public void setBaseOutputName(String baseOutputName) {
    this.baseOutputName = baseOutputName;
  }

}
