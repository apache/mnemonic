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

package org.apache.mnemonic.hadoop.mapreduce;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.mnemonic.ConfigurationException;
import org.apache.mnemonic.Durable;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.NonVolatileMemAllocator;
import org.apache.mnemonic.OutOfHybridMemory;
import org.apache.mnemonic.Utils;
import org.apache.mnemonic.hadoop.MneConfigHelper;
import org.apache.mnemonic.collections.DurableSinglyLinkedList;
import org.apache.mnemonic.collections.DurableSinglyLinkedListFactory;

public class MneMapreduceRecordWriter<V> extends RecordWriter<NullWritable, V>
    implements MneDurableComputable<NonVolatileMemAllocator> {

  protected Configuration m_conf;
  protected TaskAttemptContext m_context;
  protected NonVolatileMemAllocator m_act;
  protected Iterator<V> m_iter;
  protected long m_poolsz;
  protected long m_slotkeyid;
  protected DurableType[] m_gtypes;
  protected EntityFactoryProxy[] m_efproxies;
  protected String m_msvrname;
  protected long m_poolidx = 0;
  protected String m_outbname;
  protected String m_outext;
  protected Map<V, DurableSinglyLinkedList<V>> m_recordmap;
  protected boolean m_newpool;
  protected Pair<DurableType[], EntityFactoryProxy[]> m_recparmpair;
  protected DurableSinglyLinkedList<V> m_listnode;

  public MneMapreduceRecordWriter(TaskAttemptContext context, String outbname, String extension) {
    this(context.getConfiguration());
    m_context = context;
    m_outbname = outbname;
    m_outext = extension;
    initNextPool();
  }

  protected MneMapreduceRecordWriter(Configuration conf) {
    m_conf = conf;
    m_msvrname = MneConfigHelper.getOutputMemServiceName(m_conf);
    m_gtypes = MneConfigHelper.getOutputDurableTypes(m_conf);
    m_efproxies = Utils.instantiateEntityFactoryProxies(MneConfigHelper.getOutputEntityFactoryProxies(m_conf));
    m_recparmpair = Utils.shiftDurableParams(m_gtypes, m_efproxies, 1);
    m_slotkeyid = MneConfigHelper.getOutputSlotKeyId(m_conf);
    m_poolsz = MneConfigHelper.getOutputMemPoolSize(conf);
    m_recordmap = new HashMap<V, DurableSinglyLinkedList<V>>();
    if (m_gtypes.length < 1) {
      throw new ConfigurationException("The durable type of record parameters does not exist");
    } else {
      if (DurableType.DURABLE == m_gtypes[0]
          && m_efproxies.length < 1) { /* T.B.D. BUFFER & CHUNK */
        throw new ConfigurationException("The durable entity proxy of record parameters does not exist");
      }
    }
  }

  protected Path genNextPoolPath() {
    Path ret = new Path(FileOutputFormat.getOutputPath(m_context),
        FileOutputFormat.getUniqueFile(m_context, String.format("%s-%05d", m_outbname, ++m_poolidx), m_outext));
    return ret;
  }

  protected void initNextPool() {
    if (m_act != null) {
      m_act.close();
    }
    Path outpath = genNextPoolPath();
    m_act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService(m_msvrname), m_poolsz,
        outpath.toString(), true);
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
  protected V createDurableObjectRecord() {
    V ret = null;
    ret = (V) m_efproxies[0].create(m_act, m_recparmpair.getRight(), m_recparmpair.getLeft(), false);
    return ret;
  }

  public V newDurableObjectRecord() {
    V ret = null;
    DurableSinglyLinkedList<V> nv = null;
    try {
      nv = createDurableNode();
      ret = createDurableObjectRecord();
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
        ret = createDurableObjectRecord();
      } catch (OutOfHybridMemory ee) {
        if (nv != null) {
          nv.destroy();
        }
        if (ret != null) {
          ((Durable) ret).destroy();
        }
      }
    }
    if (ret != null) {
      m_recordmap.put(ret, nv);
    }
    return ret;
  }

  protected DurableSinglyLinkedList<V> createDurableNode() {
    DurableSinglyLinkedList<V> ret = null;
    ret = DurableSinglyLinkedListFactory.create(m_act, m_efproxies, m_gtypes, false);
    return ret;
  }

  @Override
  public void write(NullWritable nullWritable, V v) throws IOException {
    DurableSinglyLinkedList<V> nv = null;
    if (null == v) {
      return;
    }
    if (DurableType.DURABLE == m_gtypes[0]) {
      if (m_recordmap.containsKey(v)) {
        nv = m_recordmap.remove(v);
      } else {
        throw new RuntimeException("The record hasn't been created by newDurableObjectRecord()");
      }
    } else {
      try {
        nv = createDurableNode();
      } catch (OutOfHybridMemory e) {
        initNextPool();
        nv = createDurableNode();
      }
    }
    if (nv != null) {
      nv.setItem(v, false);
    }
    if (m_newpool) {
      m_act.setHandler(m_slotkeyid, nv.getHandler());
      m_newpool = false;
    } else {
      m_listnode.setNext(nv, false);
    }
    m_listnode = nv;
  }

  @Override
  public void close(TaskAttemptContext taskAttemptContext) throws IOException {
    for (V k : m_recordmap.keySet()) {
      m_recordmap.get(k).destroy();
      ((Durable) k).destroy();
    }
    m_act.close();
  }
}
