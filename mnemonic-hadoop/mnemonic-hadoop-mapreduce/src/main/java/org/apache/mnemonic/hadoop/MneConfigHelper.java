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

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.mnemonic.ConfigurationException;
import org.apache.mnemonic.DurableType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Define the configuration helper.
 */
public class MneConfigHelper {

  public static final String DEFAULT_OUTPUT_CONFIG_PREFIX = "mnemonic.output.";
  public static final String DEFAULT_INPUT_CONFIG_PREFIX = "mnemonic.input.";
  public static final String DURABLE_TYPES = "durable.types";
  public static final String ENTITY_FACTORY_PROXIES = "entity.factory.proxies.class";
  public static final String SLOT_KEY_ID = "slot.key.id";
  public static final String MEM_SERVICE_NAME = "mem.service.name";
  public static final String MEM_POOL_SIZE = "mem.pool.size";
  public static final long DEFAULT_OUTPUT_MEM_POOL_SIZE = 1024L * 1024 * 1024 * 4;
  public static final String DEFAULT_NAME_PART = "part";
  public static final String DEFAULT_FILE_EXTENSION = ".mne";
  public static final String BASE_OUTPUT_NAME = "mapreduce.output.basename";
  public static final String DIR = "dir";

  private static final Logger LOGGER = LoggerFactory.getLogger(MneConfigHelper.class);

  public static String getConfigName(String prefix, String partname) {
    prefix = null == prefix ? "" : prefix;
    return prefix + partname;
  }

  public static String getBaseOutputName(Configuration conf, String prefix) {
    return conf.get(getConfigName(prefix, BASE_OUTPUT_NAME), DEFAULT_NAME_PART);
  }

  public static void setBaseOutputName(Configuration conf, String prefix, String basename) {
    conf.set(getConfigName(prefix, BASE_OUTPUT_NAME), basename);
  }

  public static void setDurableTypes(Configuration conf, String prefix, DurableType[] dtypes) {
    String val = StringUtils.join(dtypes, ",");
    conf.set(getConfigName(prefix, DURABLE_TYPES), val);
  }

  public static DurableType[] getDurableTypes(Configuration conf, String prefix) {
    List<DurableType> ret = new ArrayList<>();
    String val = conf.get(getConfigName(prefix, DURABLE_TYPES));
    String[] vals = StringUtils.split(val, ",");
    if (null != vals) {
      for (String itm : vals) {
        ret.add(DurableType.valueOf(itm));
      }
    }
    return ret.toArray(new DurableType[0]);
  }

  public static void setEntityFactoryProxies(Configuration conf, String prefix, Class<?>[] proxies) {
    List<String> vals = new ArrayList<>();
    for (Class<?> itm : proxies) {
      vals.add(itm.getName());
    }
    conf.setStrings(getConfigName(prefix, ENTITY_FACTORY_PROXIES), vals.toArray(new String[0]));
  }

  public static Class<?>[] getEntityFactoryProxies(Configuration conf, String prefix) {
    List<Class<?>> ret = new ArrayList<>();
    String[] vals = conf.getStrings(getConfigName(prefix, ENTITY_FACTORY_PROXIES));
    String clsname = null;
    try {
      if (null != vals) {
        for (String itm : vals) {
          clsname = itm;
          ret.add(Class.forName(itm));
        }
      }
    } catch (ClassNotFoundException | NoClassDefFoundError e) {
      throw new RuntimeException(String.format("Unable to find class '%s'", clsname), e);
    }
    return ret.toArray(new Class<?>[0]);
  }

  public static void setSlotKeyId(Configuration conf, String prefix, long keyid) {
    conf.setLong(getConfigName(prefix, SLOT_KEY_ID), keyid);
  }

  public static long getSlotKeyId(Configuration conf, String prefix) {
    return conf.getLong(getConfigName(prefix, SLOT_KEY_ID), 0L);
  }

  public static void setMemServiceName(Configuration conf, String prefix, String name) {
    conf.set(getConfigName(prefix, MEM_SERVICE_NAME), name);
  }

  public static String getMemServiceName(Configuration conf, String prefix) {
    String ret = conf.get(getConfigName(prefix, MEM_SERVICE_NAME));
    if (null == ret) {
      throw new ConfigurationException("You must set the mem service name");
    }
    return ret;
  }

  public static void setMemPoolSize(Configuration conf, String prefix, long size) {
    conf.setLong(getConfigName(prefix, MEM_POOL_SIZE), size);
  }

  public static long getMemPoolSize(Configuration conf, String prefix) {
    return conf.getLong(getConfigName(prefix, MEM_POOL_SIZE), DEFAULT_OUTPUT_MEM_POOL_SIZE);
  }
  
  public static String getDir(Configuration conf, String prefix) {
    return conf.get(getConfigName(prefix, DIR));
  }

  public static void setDir(Configuration conf, String prefix, String dirname) {
    conf.set(getConfigName(prefix, DIR), dirname);
  }

}
