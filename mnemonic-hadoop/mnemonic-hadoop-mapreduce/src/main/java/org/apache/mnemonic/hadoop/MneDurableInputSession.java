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

import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.mnemonic.ConfigurationException;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.NonVolatileMemAllocator;
import org.apache.mnemonic.Utils;
import org.apache.mnemonic.collections.DurableSinglyLinkedList;
import org.apache.mnemonic.collections.DurableSinglyLinkedListFactory;

public class MneDurableInputSession<V>
    implements MneInputSession<V>, MneDurableComputable<NonVolatileMemAllocator> {

  private TaskAttemptContext taskAttemptContext;
  private Configuration configuration;
  private String serviceName;
  private DurableType[] durableTypes;
  private EntityFactoryProxy[] entityFactoryProxies;
  private long slotKeyId;

  protected long m_handler;
  protected NonVolatileMemAllocator m_act;


  public MneDurableInputSession(TaskAttemptContext taskAttemptContext) {
    setTaskAttemptContext(taskAttemptContext);
    setConfiguration(taskAttemptContext.getConfiguration());
  }

  public MneDurableInputSession(Configuration configuration) {
    setConfiguration(configuration);
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
    if (getConfiguration() == null) {
      throw new ConfigurationException("configuration has not yet been set");
    }
    Configuration conf = getConfiguration();
    setServiceName(MneConfigHelper.getMemServiceName(conf, MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX));
    setDurableTypes(MneConfigHelper.getDurableTypes(conf, MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX));
    setEntityFactoryProxies(Utils.instantiateEntityFactoryProxies(
        MneConfigHelper.getEntityFactoryProxies(conf, MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX)));
    setSlotKeyId(MneConfigHelper.getSlotKeyId(conf, MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX));
    validateConfig();
  }

  @Override
  public void initialize(Path path) {
    m_act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService(getServiceName()), 1024000L,
        path.toString(), true);
    m_handler = m_act.getHandler(getSlotKeyId());
  }

  @Override
  public Iterator<V> iterator() {
    Iterator<V> iter;
    DurableSinglyLinkedList<V> dsllist;
    dsllist = DurableSinglyLinkedListFactory.restore(m_act, getEntityFactoryProxies(), getDurableTypes(), m_handler,
        false);
    iter = dsllist.iterator();
    return iter;
  }

  @Override
  public void close() {
    m_act.close();
  }

  public TaskAttemptContext getTaskAttemptContext() {
    return taskAttemptContext;
  }

  public void setTaskAttemptContext(TaskAttemptContext taskAttemptContext) {
    this.taskAttemptContext = taskAttemptContext;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public DurableType[] getDurableTypes() {
    return durableTypes;
  }

  public void setDurableTypes(DurableType[] durableTypes) {
    this.durableTypes = durableTypes;
  }

  public EntityFactoryProxy[] getEntityFactoryProxies() {
    return entityFactoryProxies;
  }

  public void setEntityFactoryProxies(EntityFactoryProxy[] entityFactoryProxies) {
    this.entityFactoryProxies = entityFactoryProxies;
  }

  public long getSlotKeyId() {
    return slotKeyId;
  }

  public void setSlotKeyId(long slotKeyId) {
    this.slotKeyId = slotKeyId;
  }

  @Override
  public NonVolatileMemAllocator getAllocator() {
    return m_act;
  }

  @Override
  public long getHandler() {
    return m_handler;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }
}
