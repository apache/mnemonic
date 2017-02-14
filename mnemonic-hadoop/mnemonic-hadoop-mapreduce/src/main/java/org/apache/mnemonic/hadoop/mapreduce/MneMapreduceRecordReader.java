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
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.NonVolatileMemAllocator;
import org.apache.mnemonic.Utils;
import org.apache.mnemonic.collections.DurableSinglyLinkedList;
import org.apache.mnemonic.collections.DurableSinglyLinkedListFactory;
import org.apache.mnemonic.hadoop.MneConfigHelper;

/**
 * This record reader implements the org.apache.hadoop.mapreduce API.
 * @param <V> the type of the data item
 */
public class MneMapreduceRecordReader<V>
    extends org.apache.hadoop.mapreduce.RecordReader<NullWritable, V>
    implements MneDurableComputable<NonVolatileMemAllocator> {

  protected Configuration m_conf;
  protected TaskAttemptContext m_context;
  protected NonVolatileMemAllocator m_act;
  protected Iterator<V> m_iter;
  protected long m_slotkeyid;
  protected long m_handler = 0L;
  protected DurableType[] m_gtypes;
  protected EntityFactoryProxy[] m_efproxies;
  protected String m_msvrname;

  public MneMapreduceRecordReader() {
  }

  @Override
  public void close() throws IOException {
    m_act.close();
  }

  @Override
  public void initialize(InputSplit inputSplit,
                         TaskAttemptContext context) {
    FileSplit split = (FileSplit) inputSplit;
    m_context = context;
    m_conf = m_context.getConfiguration();
    m_msvrname = MneConfigHelper.getMemServiceName(m_conf, MneConfigHelper.INPUT_CONFIG_PREFIX_DEFAULT);
    m_gtypes = MneConfigHelper.getDurableTypes(m_conf, MneConfigHelper.INPUT_CONFIG_PREFIX_DEFAULT);
    m_efproxies = Utils.instantiateEntityFactoryProxies(
        MneConfigHelper.getEntityFactoryProxies(m_conf, MneConfigHelper.INPUT_CONFIG_PREFIX_DEFAULT));
    m_slotkeyid = MneConfigHelper.getSlotKeyId(m_conf, MneConfigHelper.INPUT_CONFIG_PREFIX_DEFAULT);
    
    DurableSinglyLinkedList<V> dsllist;

    m_act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService(m_msvrname), 1024000L,
        split.getPath().toString(), true);
    m_handler = m_act.getHandler(m_slotkeyid);
    dsllist = DurableSinglyLinkedListFactory.restore(m_act, m_efproxies, 
        m_gtypes, m_handler, false);
    m_iter = dsllist.iterator();
  }

  @Override
  public boolean nextKeyValue() throws IOException, InterruptedException {
    return m_iter.hasNext();
  }

  @Override
  public NullWritable getCurrentKey() throws IOException, InterruptedException {
    return NullWritable.get();
  }

  @Override
  public V getCurrentValue() throws IOException, InterruptedException {
    return m_iter.next();
  }

  @Override
  public float getProgress() throws IOException {
    return 0.5f; /* TBD */
  }

  @Override
  public NonVolatileMemAllocator getAllocator() {
    return m_act;
  }

  @Override
  public long getHandler() {
    return m_handler;
  }

}
