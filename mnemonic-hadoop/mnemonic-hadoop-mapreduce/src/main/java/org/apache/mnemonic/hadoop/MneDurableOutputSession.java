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
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.NonVolatileMemAllocator;
import org.apache.mnemonic.Utils;
import org.apache.mnemonic.sessions.DurableOutputSession;

import java.text.NumberFormat;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.TaskID;

public class MneDurableOutputSession<V>
    extends DurableOutputSession<V, NonVolatileMemAllocator> {

  private String baseOutputName;
  private Path outputPath;
  private TaskAttemptContext taskAttemptContext;
  private Configuration configuration;

  public MneDurableOutputSession(TaskAttemptContext taskAttemptContext,
      Configuration configuration, String prefix) {
    if (null == taskAttemptContext && null == configuration) {
      throw new ConfigurationException("Session is not configured properly");
    }
    if (null != taskAttemptContext) {
      setTaskAttemptContext(taskAttemptContext);
      setConfiguration(taskAttemptContext.getConfiguration());
    } else {
      setConfiguration(configuration);
    }
    initialize(prefix);
  }

  public void initialize(String prefix) {
    readConfig(prefix);
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

  public void readConfig(String prefix) {
    Configuration conf = getConfiguration();
    if (conf == null) {
      throw new ConfigurationException("Configuration has not yet been set");
    }
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
    Path ret = new Path(getOutputDir(),
        getUniqueName(String.format("%s-%05d", getBaseOutputName(), ++m_poolidx), 
            MneConfigHelper.DEFAULT_FILE_EXTENSION));
    return ret;
  }

  protected Path getOutputDir() {
    String name = MneConfigHelper.getDir(getConfiguration(), MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX);
    return name == null ? null : new Path(name);
  }

  protected String getUniqueName(String name, String extension) {
    int partition;
    
    NumberFormat numberFormat = NumberFormat.getInstance();
    numberFormat.setMinimumIntegerDigits(5);
    numberFormat.setGroupingUsed(false);
    
    if (null != getTaskAttemptContext()) {
      TaskID taskId = getTaskAttemptContext().getTaskAttemptID().getTaskID();
      partition = taskId.getId();
    } else {
      partition = getConfiguration().getInt(JobContext.TASK_PARTITION, -1);
    } 
    if (partition == -1) {
      throw new IllegalArgumentException("This method can only be called from an application");
    }
    
    String taskType = getConfiguration().getBoolean(JobContext.TASK_ISMAP, JobContext.DEFAULT_TASK_ISMAP) ? "m" : "r";
    
    StringBuilder result = new StringBuilder();
    result.append(name);
    result.append('-');
    result.append(taskType);
    result.append('-');
    result.append(numberFormat.format(partition));
    result.append(extension);
    return result.toString();
    
  }

  @Override
  protected boolean initNextPool() {
    boolean ret = false;
    if (m_act != null) {
      m_act.close();
      m_act = null;
    }
    setOutputPath(genNextPoolPath());
    m_act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService(getServiceName()), getPoolSize(),
        getOutputPath().toString(), true);
    if (null != m_act) {
      m_newpool = true;
      ret = true;
    }
    return ret;
  }

  public Path getOutputPath() {
    return outputPath;
  }

  public void setOutputPath(Path outputPath) {
    this.outputPath = outputPath;
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

  public Configuration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

}
