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

/**
 * This class extends DurableInputSession and is responsible for handling 
 * non-volatile memory-based input sessions using Hadoop’s TaskAttemptContext.
 * 
 * @param <V> The type of elements stored in the session (must be durable).
 */
public class MneDurableInputSession<V>
    extends DurableInputSession<V, NonVolatileMemAllocator, Void, Void> {

  private TaskAttemptContext taskAttemptContext;
  private Configuration configuration;
  private Iterator<String> m_fp_iter;

  /**
   * Constructor initializes the session using either a TaskAttemptContext or
   * a standalone Configuration object.
   * 
   * @param taskAttemptContext Task-specific context (optional).
   * @param configuration      Hadoop Configuration.
   * @param paths              Array of file paths for input.
   * @param prefix             Configuration prefix for memory service.
   */
  public MneDurableInputSession(TaskAttemptContext taskAttemptContext,
      Configuration configuration, Path[] paths, String prefix) {
    // Check if both the TaskAttemptContext and Configuration are null
    if (null == taskAttemptContext && null == configuration) {
      throw new ConfigurationException("Session is not configured properly");
    }
    
    // If taskAttemptContext is provided, use it to set configuration
    if (null != taskAttemptContext) {
      setTaskAttemptContext(taskAttemptContext);
      setConfiguration(taskAttemptContext.getConfiguration());
    } else {
      setConfiguration(configuration);
    }
    
    // Initialize file paths and configuration
    initialize(paths, prefix);
  }

  /**
   * Initializes file paths and reads the configuration prefix for the session.
   * 
   * @param paths  Array of file paths.
   * @param prefix Configuration prefix for memory service.
   */
  public void initialize(Path[] paths, String prefix) {
    List<String> fpathlist = new ArrayList<String>();
    
    // Validate each path to ensure it's a regular file
    for (Path p : paths) {
      if (!Files.isRegularFile(Paths.get(p.toString()), LinkOption.NOFOLLOW_LINKS)) {
        throw new UnsupportedOperationException();
      }
      fpathlist.add(p.toString());
    }
    
    // Set up an iterator for the file paths
    m_fp_iter = fpathlist.iterator();
    
    // Read the configuration
    readConfig(prefix);
  }

  /**
   * Validates the configuration for the session, checking if durable types 
   * and entity factory proxies are properly set.
   */
  public void validateConfig() {
    if (getDurableTypes().length < 1) {
      throw new ConfigurationException("The durable type of record parameters does not exist");
    } else {
      // Check if the first durable type is a durable entity and if proxies are set
      if (DurableType.DURABLE == getDurableTypes()[0]
          && getEntityFactoryProxies().length < 1) {
        throw new ConfigurationException("The durable entity proxy of record parameters does not exist");
      }
    }
  }

  /**
   * Reads the configuration using a prefix to set service name, durable types,
   * entity factory proxies, and slot key ID for memory allocation.
   * 
   * @param prefix Configuration prefix.
   */
  public void readConfig(String prefix) {
    Configuration conf = getConfiguration();
    
    // Check if configuration is set
    if (conf == null) {
      throw new ConfigurationException("Configuration has not yet been set");
    }
    
    // Set configuration values for memory service, durable types, entity proxies, and slot key ID
    setServiceName(MneConfigHelper.getMemServiceName(conf, MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX));
    setDurableTypes(MneConfigHelper.getDurableTypes(conf, MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX));
    setEntityFactoryProxies(Utils.instantiateEntityFactoryProxies(
        MneConfigHelper.getEntityFactoryProxies(conf, MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX)));
    setSlotKeyId(MneConfigHelper.getSlotKeyId(conf, MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX));
    
    // Validate the configuration to ensure it's correct
    validateConfig();
  }

  /**
   * Initialize the session iterator. In this case, it's a simple success check.
   * 
   * @param sessiter The session iterator to initialize.
   * @return Always returns true since no additional work is required for init.
   */
  @Override
  protected boolean init(SessionIterator<V, NonVolatileMemAllocator, Void, Void> sessiter) {
    return true;
  }

  /**
   * Prepares the next memory pool for the session and restores data if available.
   * 
   * @param sessiter The session iterator used to restore and iterate over durable lists.
   * @return True if initialization is successful, false otherwise.
   */
  @Override
  protected boolean initNextPool(SessionIterator<V, NonVolatileMemAllocator, Void, Void> sessiter) {
    boolean ret = false;
    
    // Close and reset the allocator if already initialized
    if (sessiter.getAllocator() != null) {
      sessiter.getAllocator().close();
      sessiter.setAllocator(null);
    }
    
    // If there are more files to process
    if (null != m_fp_iter && m_fp_iter.hasNext()) {
      // Create a new memory allocator with the next file path
      sessiter.setAllocator(new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService(
          getServiceName()), 1024000L, m_fp_iter.next(), false));
      
      // Check if the allocator was successfully created
      if (null != sessiter.getAllocator()) {
        sessiter.setHandler(sessiter.getAllocator().getHandler(getSlotKeyId()));
        
        // If the handler is valid, restore the durable singly linked list
        if (0L != sessiter.getHandler()) {
          DurableSinglyLinkedList<V> dsllist = DurableSinglyLinkedListFactory.restore(
              sessiter.getAllocator(), getEntityFactoryProxies(), getDurableTypes(), sessiter.getHandler(), false);
          
          // Set the iterator if the list was restored successfully
          if (null != dsllist) {
            sessiter.setIterator(dsllist.iterator());
            ret = null != sessiter.getIterator();
          }
        }
      }
    }
    return ret;
  }

  // Getter for TaskAttemptContext
  public TaskAttemptContext getTaskAttemptContext() {
    return taskAttemptContext;
  }

  // Setter for TaskAttemptContext
  public void setTaskAttemptContext(TaskAttemptContext taskAttemptContext) {
    this.taskAttemptContext = taskAttemptContext;
  }

  // Getter for Configuration
  public Configuration getConfiguration() {
    return configuration;
  }

  // Setter for Configuration
  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }
}
