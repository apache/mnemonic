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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.mnemonic.ConfigurationException;
import org.apache.mnemonic.DurableType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Define the configuration helper.
 */
public class MneConfigHelper {

  private static final String INPUT_DURABLE_TYPES_CONFIG = "mnemonic.input.durable.types";
  private static final String INPUT_ENTITY_FACTORY_PROXIES = "mnemonic.input.entity.factory.proxies.class";
  private static final String OUTPUT_DURABLE_TYPES_CONFIG = "mnemonic.output.durable.types";
  private static final String OUTPUT_ENTITY_FACTORY_PROXIES = "mnemonic.output.entity.factory.proxies.class";
  private static final String INPUT_SLOT_KEY_ID = "mnemonic.input.slot.key.id";
  private static final String OUTPUT_SLOT_KEY_ID = "mnemonic.output.slot.key.id";
  private static final String INPUT_MEM_SERVICE_NAME = "mnemonic.input.mem.service.name";
  private static final String OUTPUT_MEM_SERVICE_NAME = "mnemonic.output.mem.service.name";
  private static final String OUTPUT_MEM_POOL_SIZE = "mnemonic.output.mem.pool.size";
//  private static final String RECORD_FACTORY_CLASS = "mnemonic.record_factory_class";
  private static final long DEFAULT_OUTPUT_MEM_POOL_SIZE = 1024L * 1024 * 1024 * 4;
  public static final String FILE_EXTENSION = ".mne";

  private static final Logger LOGGER = LoggerFactory.getLogger(MneConfigHelper.class);

  public static void setInputDurableTypes(Configuration conf, DurableType[] dtypes) {
    String val = StringUtils.join(dtypes, ",");
    conf.set(INPUT_DURABLE_TYPES_CONFIG, val);
  }

  public static DurableType[] getInputDurableTypes(Configuration conf) {
    List<DurableType> ret = new ArrayList<>();
    String val = conf.get(INPUT_DURABLE_TYPES_CONFIG);
    String[] vals = StringUtils.split(val, ",");
    for (String itm : vals) {
      ret.add(DurableType.valueOf(itm));
    }
    return ret.toArray(new DurableType[0]);
  }

  public static void setOutputDurableTypes(Configuration conf, DurableType[] dtypes) {
    String val = StringUtils.join(dtypes, ",");
    conf.set(OUTPUT_DURABLE_TYPES_CONFIG, val);
  }

  public static DurableType[] getOutputDurableTypes(Configuration conf) {
    List<DurableType> ret = new ArrayList<>();
    String val = conf.get(OUTPUT_DURABLE_TYPES_CONFIG);
    String[] vals = StringUtils.split(val, ",");
    for (String itm : vals) {
      ret.add(DurableType.valueOf(itm));
    }
    return ret.toArray(new DurableType[0]);
  }

  public static void setInputEntityFactoryProxies(Configuration conf, Class<?>[] proxies) {
    List<String> vals = new ArrayList<>();
    for (Class<?> itm : proxies) {
      vals.add(itm.getName());
    }
    conf.setStrings(INPUT_ENTITY_FACTORY_PROXIES, vals.toArray(new String[0]));
  }

  public static Class<?>[] getInputEntityFactoryProxies(Configuration conf) {
    List<Class<?>> ret = new ArrayList<>();
    String[] vals = conf.getStrings(INPUT_ENTITY_FACTORY_PROXIES);
    String clsname = null;
    try {
      for (String itm : vals) {
        clsname = itm;
        ret.add(Class.forName(itm));
      }
    } catch (ClassNotFoundException | NoClassDefFoundError e) {
      throw new RuntimeException(String.format("Unable to find class '%s'", clsname), e);
    }
    return ret.toArray(new Class<?>[0]);
  }

  public static void setOutputEntityFactoryProxies(Configuration conf, Class<?>[] proxies) {
    List<String> vals = new ArrayList<>();
    for (Class<?> itm : proxies) {
      vals.add(itm.getName());
    }
    conf.setStrings(OUTPUT_ENTITY_FACTORY_PROXIES, vals.toArray(new String[0]));
  }

  public static Class<?>[] getOutputEntityFactoryProxies(Configuration conf) {
    List<Class<?>> ret = new ArrayList<>();
    String[] vals = conf.getStrings(OUTPUT_ENTITY_FACTORY_PROXIES);
    String clsname = null;
    try {
      for (String itm : vals) {
        clsname = itm;
        ret.add(Class.forName(itm));
      }
    } catch (ClassNotFoundException | NoClassDefFoundError e) {
      throw new RuntimeException(String.format("Unable to find class '%s'", clsname), e);
    }
    return ret.toArray(new Class<?>[0]);
  }

  public static void setInputSlotKeyId(Configuration conf, long keyid) {
    conf.setLong(INPUT_SLOT_KEY_ID, keyid);
  }

  public static long getInputSlotKeyId(Configuration conf) {
    return conf.getLong(INPUT_SLOT_KEY_ID, 0L);
  }

  public static void setOutputSlotKeyId(Configuration conf, long keyid) {
    conf.setLong(OUTPUT_SLOT_KEY_ID, keyid);
  }

  public static long getOutputSlotKeyId(Configuration conf) {
    return conf.getLong(OUTPUT_SLOT_KEY_ID, 0L);
  }

  public static void setInputMemServiceName(Configuration conf, String name) {
    conf.set(INPUT_MEM_SERVICE_NAME, name);
  }

  public static String getInputMemServiceName(Configuration conf) {
    String ret = conf.get(INPUT_MEM_SERVICE_NAME);
    if (null == ret) {
      throw new ConfigurationException("You must set the input mem service name");
    }
    return ret;
  }

  public static void setOutputMemServiceName(Configuration conf, String name) {
    conf.set(OUTPUT_MEM_SERVICE_NAME, name);
  }

  public static String getOutputMemServiceName(Configuration conf) {
    String ret = conf.get(OUTPUT_MEM_SERVICE_NAME);
    if (null == ret) {
      throw new ConfigurationException("You must set the output mem service name");
    }
    return ret;
  }

  public static void setOutputMemPoolSize(Configuration conf, long size) {
    conf.setLong(OUTPUT_MEM_POOL_SIZE, size);
  }

  public static long getOutputMemPoolSize(Configuration conf) {
    return conf.getLong(OUTPUT_MEM_POOL_SIZE, DEFAULT_OUTPUT_MEM_POOL_SIZE);
  }

//
//  public static Class<?> getRecordFactory(Configuration conf) {
//    Class<?> ret;
//    String clsname = conf.get(RECORD_FACTORY_CLASS);
//    try {
//      ret = Class.forName(clsname);
//    } catch (ClassNotFoundException | NoClassDefFoundError e) {
//      throw new RuntimeException(String.format("Unable to find record factory class '%s'", clsname), e);
//    }
//    return ret;
//  }
//
//  public static void setRecordFactory(Configuration conf, Class<?> recf) {
//    conf.setStrings(RECORD_FACTORY_CLASS, recf.getName());
//  }

}
