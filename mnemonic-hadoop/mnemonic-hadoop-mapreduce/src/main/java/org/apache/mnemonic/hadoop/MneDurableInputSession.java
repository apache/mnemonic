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

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.mnemonic.ConfigurationException;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.NonVolatileMemAllocator;
import org.apache.mnemonic.Utils;
import org.apache.mnemonic.collections.DurableSinglyLinkedList;
import org.apache.mnemonic.collections.DurableSinglyLinkedListFactory;
import org.apache.mnemonic.sessions.DurableInputSession;
import org.apache.mnemonic.sessions.SessionIterator;

public class MneDurableInputSession<V>
    extends DurableInputSession<V, NonVolatileMemAllocator, Void, Void> {

  private TaskAttemptContext taskAttemptContext;
  private Configuration configuration;
  private Iterator<String> m_fp_iter;

  public MneDurableInputSession(TaskAttemptContext taskAttemptContext,
      Configuration configuration, Path[] paths, String prefix) {
    if (null == taskAttemptContext && null == configuration) {
      throw new ConfigurationException("Session is not configured properly");
    }
    if (null != taskAttemptContext) {
      setTaskAttemptContext(taskAttemptContext);
      setConfiguration(taskAttemptContext.getConfiguration());
    } else {
      setConfiguration(configuration);
    }
    initialize(paths, prefix);
  }

  public void initialize(Path[] paths, String prefix) {
    List<String> fpathlist = new ArrayList<String>();
    for (Path p : paths) {
      if (!Files.isRegularFile(Paths.get(p.toString()), LinkOption.NOFOLLOW_LINKS)) {
        throw new UnsupportedOperationException();
      }
      fpathlist.add(p.toString());
    }
    m_fp_iter = fpathlist.iterator();
    readConfig(prefix);
  }

  public void validateConfig() {
    if (getDurableTypes().length < 1) {
      throw new ConfigurationException("The durable type of record parameters does not exist");
    } else {
      if (DurableType.DURABLE == getDurableTypes()[0]
          && getEntityFactoryProxies().length < 1) {
        throw new ConfigurationException("The durable entity proxy of record parameters does not exist");
      }
    }
  }

  public void readConfig(String prefix) {
    Configuration conf = getConfiguration();
    if (conf == null) {
      throw new ConfigurationException("Configuration has not yet been set");
    }
    setServiceName(MneConfigHelper.getMemServiceName(conf, MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX));
    setDurableTypes(MneConfigHelper.getDurableTypes(conf, MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX));
    setEntityFactoryProxies(Utils.instantiateEntityFactoryProxies(
        MneConfigHelper.getEntityFactoryProxies(conf, MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX)));
    setSlotKeyId(MneConfigHelper.getSlotKeyId(conf, MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX));
    validateConfig();
  }

  @Override
  protected boolean init(SessionIterator<V, NonVolatileMemAllocator, Void, Void> sessiter) {
    return true;
  }

  @Override
  protected boolean initNextPool(SessionIterator<V, NonVolatileMemAllocator, Void, Void> sessiter) {
    boolean ret = false;
    if (sessiter.getAllocator() != null) {
      sessiter.getAllocator().close();
      sessiter.setAllocator(null);
    }
    if (null != m_fp_iter && m_fp_iter.hasNext()) {
      sessiter.setAllocator(new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService(
          getServiceName()), 1024000L, m_fp_iter.next(), false));
      if (null != sessiter.getAllocator()) {
        sessiter.setHandler(sessiter.getAllocator().getHandler(getSlotKeyId()));
        if (0L != sessiter.getHandler()) {
          DurableSinglyLinkedList<V> dsllist = DurableSinglyLinkedListFactory.restore(
              sessiter.getAllocator(), getEntityFactoryProxies(), getDurableTypes(), sessiter.getHandler(), false);
          if (null != dsllist) {
            sessiter.setIterator(dsllist.iterator());
            ret = null != sessiter.getIterator();
          }
        }
      }
    }
    return ret;
  }

  public TaskAttemptContext getTaskAttemptContext() {
    return taskAttemptContext;
  }

  public void setTaskAttemptContext(TaskAttemptContext taskAttemptContext) {
    this.taskAttemptContext = taskAttemptContext;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }
}
