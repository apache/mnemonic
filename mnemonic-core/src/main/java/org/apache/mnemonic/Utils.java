/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mnemonic;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.mnemonic.service.computing.GeneralComputingService;
import org.apache.mnemonic.service.memory.NonVolatileMemoryAllocatorService;
import org.apache.mnemonic.service.memory.VolatileMemoryAllocatorService;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.ServiceLoader;
import java.util.UUID;
import java.lang.reflect.InvocationTargetException;

/**
 * <p>
 * Utilities for project.
 * </p>
 *
 */
@SuppressWarnings("restriction")
public class Utils {

  public static final String ANSI_RESET = "\u001B[0m";
  public static final String ANSI_BLACK = "\u001B[30m";
  public static final String ANSI_RED = "\u001B[31m";
  public static final String ANSI_GREEN = "\u001B[32m";
  public static final String ANSI_YELLOW = "\u001B[33m";
  public static final String ANSI_BLUE = "\u001B[34m";
  public static final String ANSI_PURPLE = "\u001B[35m";
  public static final String ANSI_CYAN = "\u001B[36m";
  public static final String ANSI_WHITE = "\u001B[37m";

  @SuppressWarnings({"restriction", "UseOfSunClasses"})
  private static sun.misc.Unsafe m_unsafe = null;

  private static ServiceLoader<VolatileMemoryAllocatorService> m_vmasvcloader = null;
  private static ServiceLoader<NonVolatileMemoryAllocatorService> m_nvmasvcloader = null;
  private static ServiceLoader<GeneralComputingService> m_gcompsvcloader = null;

  /**
   * retrieve a volatile memory allocator service
   *
   * @param id
   *          specify a name of allocator to retrieve
   *
   * @return the volatile memory allocator service instance
   */
  public static VolatileMemoryAllocatorService getVolatileMemoryAllocatorService(String id) {
    return getVolatileMemoryAllocatorService(id, true);
  }

  /**
   * retrieve a volatile memory allocator service
   *
   * @param id
   *          specify a name of allocator to retrieve
   *
   * @param allownvmsvc
   *          specify whether allow to treat non-volatile memory allocator as
   *          volatile one during searching
   *
   * @return the volatile memory allocator service instance
   */
  public static VolatileMemoryAllocatorService getVolatileMemoryAllocatorService(String id, boolean allownvmsvc) {
    VolatileMemoryAllocatorService ret = null;
    if (null == m_vmasvcloader) {
      m_vmasvcloader = ServiceLoader.load(VolatileMemoryAllocatorService.class);
    }
    Iterator<VolatileMemoryAllocatorService> svcit = m_vmasvcloader.iterator();
    VolatileMemoryAllocatorService svc = null;
    while (null == ret && svcit.hasNext()) {
      svc = svcit.next();
      if (svc.getServiceId().equals(id)) {
        ret = svc;
      }
    }
    if (null == ret && allownvmsvc) {
      ret = getNonVolatileMemoryAllocatorService(id);
    }
    assert null != ret : "VolatileMemoryAllocatorService \'" + id + "\' not found!";
    return ret;
  }

  /**
   * retrieve a non-volatile memory allocator service
   *
   * @param id
   *          specify a name of allocator to retrieve
   *
   * @return the non-volatile memory allocator service instance
   */
  public static synchronized NonVolatileMemoryAllocatorService getNonVolatileMemoryAllocatorService(String id) {
    NonVolatileMemoryAllocatorService ret = null;
    if (null == m_nvmasvcloader) {
      m_nvmasvcloader = ServiceLoader.load(NonVolatileMemoryAllocatorService.class);
    }
    Iterator<NonVolatileMemoryAllocatorService> svcit = m_nvmasvcloader.iterator();
    NonVolatileMemoryAllocatorService svc = null;
    while (svcit.hasNext()) {
      svc = svcit.next();
      if (svc.getServiceId().equals(id)) {
        ret = svc;
        break;
      }
    }
    assert null != ret : "NonVolatileMemoryAllocatorService \'" + id + "\' not found!";
    return ret;
  }

  /**
   * retrieve a durable general computing service
   *
   * @param id
   *          specify a name of general computing to retrieve
   *
   * @return the durable general computing service instance
   */
  public static synchronized GeneralComputingService getGeneralComputingService(String id) {
    GeneralComputingService ret = null;
    if (null == m_gcompsvcloader) {
      m_gcompsvcloader = ServiceLoader.load(GeneralComputingService.class);
    }
    Iterator<GeneralComputingService> svcit = m_gcompsvcloader.iterator();
    GeneralComputingService svc = null;
    while (svcit.hasNext()) {
      svc = svcit.next();
      if (svc.getServiceId().equals(id)) {
        ret = svc;
        break;
      }
    }
    assert null != ret : "GeneralComputingService \'" + id + "\' not found!";
    return ret;
  }

  /**
   * Generates a unique name that contains current timestamp.
   *
   * @param format
   *          the template that is used to generate unique name.
   *
   * @return unique path name.
   */
  public static String genUniquePathname(String format) {
    String ret = null;
    if (null != format && !format.isEmpty()) {
      ret = String.format(format, (new SimpleDateFormat("ddMMyy-hhmmss.SSS").format(new Date())));
    }
    return ret;
  }

  /**
   * retrieve the usage of memory.
   *
   * @param timeout
   *         specify a timeout for this operation
   *
   * @return the size of memory has been occupied
   */
  public static long getMemoryUse(long timeout) {
    putOutTheGarbage(timeout);
    long totalMemory = Runtime.getRuntime().totalMemory();
    putOutTheGarbage(timeout);
    long freeMemory = Runtime.getRuntime().freeMemory();
    return (totalMemory - freeMemory);
  }

  /**
   * run garbage collections.
   */
  private static void putOutTheGarbage(long timeout) {
    collectGarbage(timeout);
    collectGarbage(timeout);
  }

  /**
   * run a garbage collection.
   *
   * @param timeout
   *         specify a timeout for this operation
   *
   */
  public static void collectGarbage(long timeout) {
    try {
      System.gc();
      Thread.sleep(timeout);
      System.runFinalization();
      Thread.sleep(timeout);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Retrieve an Unsafe object.
   *
   * @throws Exception
   *           Error when get Unsafe object from runtime
   *
   * @return an unsafe object
   */
  @SuppressWarnings({"restriction", "UseOfSunClasses"})
  public static sun.misc.Unsafe getUnsafe() throws Exception {
    if (null == m_unsafe) {
      Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
      field.setAccessible(true);
      m_unsafe = (sun.misc.Unsafe) field.get(null);
    }
    return m_unsafe;
  }

  /**
   * resize a bytebuffer with a new instance
   *
   * @param buf
   *          specify a buf to resize
   *
   * @param size
   *          specify the size for resizing
   *
   * @return the resized bytebuffer instance
   */
  public static ByteBuffer resizeByteBuffer(ByteBuffer buf, long size) {
    ByteBuffer ret = ByteBuffer.allocateDirect((int) size);
    if (ret != null) {
      if (null != buf) {
        ret.put(buf);
        ret.flip();
      }
    }
    return ret;
  }

  /**
   * create a new instance of Random using default fixed seed
   *
   * @return the instance of Random
   */
  public static Random createRandom() {
    return createRandom(0L);
  }

  /**
   * create a new instance of Random
   *
   * @param rgenseed
   *          specify a random seed
   *
   * @return the instance of Random
   */
  public static Random createRandom(long rgenseed) {
    Random ret = new Random();
    if (0L == rgenseed) {
      rgenseed = System.currentTimeMillis();
      System.out.println("Random number generator seed is " + rgenseed);
    } else {
      System.out.println("Fixed Random number generator seed is " + rgenseed);
    }
    ret.setSeed(rgenseed);
    return ret;
  }

  /**
   * generate a random string with fixed length
   *
   * @return the random string
   */
  public static String genRandomString() {
    return genRandomString(6);
  }

  /**
   * generate a random string
   *
   * @param len
   *          specify the length of this random string
   *
   * @return the random string
   */
  public static String genRandomString(int len) {
    return UUID.randomUUID().toString().replaceAll("-", "").toUpperCase().substring(0, len);
  }

  /**
   * assert the equality of two generic objects using compareTo() operator
   *
   * @param <T>
   *          the type of comparable objects
   *
   * @param actual
   *          specify a object to be compared
   *
   * @param expected
   *          specify a object to be expected
   *
   * @return true if equal according to compareTo()
   */
  public static <T extends Comparable<T>> boolean assertComparison(T actual, T expected) {
    boolean ret = false;
    if ((expected == null) && (actual == null)) {
      ret = true;
    } else if (expected != null) {
      ret = expected.compareTo(actual) == 0;
    }
    return ret;
  }

  /**
   * convert a long array to a initializer literal string.
   *
   * @param larr
   *          specify a long array to be converted
   *
   * @return a literal string represent the initializer
   */
  public static String toInitLiteral(long[] larr) {
    return Arrays.toString(larr).replaceAll("\\[", "{").replaceAll("\\]", "}");
  }

  /**
   * convert a list of long array to a initializer literal string.
   *
   * @param llarr
   *          specify a list of long array to be converted
   *
   * @return a literal string represent the initializer
   */
  public static String toInitLiteral(List<long[]> llarr) {
    List<String> slist = new ArrayList<String>();
    for (long[] larr : llarr) {
      slist.add(toInitLiteral(larr));
    }
    return "{" + StringUtils.join(slist, ",") + "}";
  }

  /**
   * retrieve a set of native field info from a list of object field info
   * according to the field id info. it forms a value info stack for native code
   * to use as one standardized parameter
   *
   * @param objstack
   *          a stack of object info retrieved from
   *          Durable.getNativeFieldInfo(), order matters
   *
   * @param fidinfostack
   *          a stack of field id in the form of (next_fid, next_level_fid)
   *          order follows objstack the last next_level_fid specifies the
   *          value's fid. the last item of next_fid could be null if there is
   *          no next node if it is null that means the last item is a object
   *          instead of node
   *
   * @return the stack of native field info
   *
   */
  public static List<long[]> getNativeParamForm(List<long[][]> objstack, long[][] fidinfostack) {
    List<long[]> ret = null;
    if (null == objstack || null == fidinfostack || fidinfostack.length != objstack.size()) {
      throw new IllegalArgumentException("Not the same depth");
    }
    ret = new ArrayList<long[]>();
    for (int idx = 0; idx < fidinfostack.length; ++idx) {
      ret.add(genNativeStackItem(objstack.get(idx), fidinfostack[idx], idx == fidinfostack.length - 1));
    }
    return ret;
  }

  /**
   * convert list form of native parameters to array form
   *
   * @param npf
   *         a list of native parameters
   *
   * @return the 2d array form of native parameter frame
   */
  public static long[][] convertTo2DArrayForm(List<long[]> npf) {
    long[][] ret = null;
    if (null != npf && npf.size() > 0) {
      ret = npf.toArray(new long[npf.size()][]);
    }
    return ret;
  }

  /**
   * generate native form of object stack parameter frame.
   *
   * @param objstack
   *          a stack of object info retrieved from
   *          Durable.getNativeFieldInfo(), order matters
   *
   * @param fidinfostack
   *          a stack of field id in the form of (next_fid, next_level_fid)
   *          order follows objstack the last next_level_fid specifies the
   *          value's fid. the last item of next_fid could be null if there is
   *          no next node if it is null that means the last item is a object
   *          instead of node
   *
   * @return the 2d array form of native parameter frame
   *
   * @see #getNativeParamForm(List, long[][])
   *
   */
  public static long[][] genNativeParamForm(List<long[][]> objstack, long[][] fidinfostack) {
    return convertTo2DArrayForm(getNativeParamForm(objstack, fidinfostack));
  }

  /**
   * generate an item of native stack.
   *
   * @param oinfo
   *          a object field info
   *
   * @param fidinfo
   *          a pair of field id info
   *
   * @param allowfidnull
   *          allow the first field id is null
   *
   * @return the native item
   */
  public static long[] genNativeStackItem(long[][] oinfo, long[] fidinfo, boolean allowfidnull) {
    long[] ret = new long[4];
    long fid;
    boolean found;
    if (fidinfo.length != 2) {
      throw new IllegalArgumentException("the length of field id array is not exactly 2");
    }
    for (int idx = 0; idx < fidinfo.length; ++idx) {
      ret[idx * 2] = -1L;
      ret[idx * 2 + 1] = 0L;
      fid = fidinfo[idx];
      if (fid <= 0) {
        if (allowfidnull && 0 == idx) {
          continue;
        } else {
          throw new IllegalArgumentException("the field id is not greater than 0");
        }
      }
      found = false;
      for (long[] finfo : oinfo) {
        if (finfo.length != 3) {
          throw new IllegalArgumentException("the length of field array is not exactly 3");
        }
        if (fid == finfo[0]) {
          ret[idx * 2] = finfo[1];
          ret[idx * 2 + 1] = finfo[2];
          found = true;
        }
      }
      if (!found) {
        throw new IllegalArgumentException("field id not found");
      }
    }
    return ret;
  }

  /**
   * instantiate an array of entity factory proxy classes.
   *
   * @param proxyclses
   *          an array of entity factory proxy classes
   *
   * @return the array of instances
   */
  public static EntityFactoryProxy[] instantiateEntityFactoryProxies(Class<?>[] proxyclses) {
    List<EntityFactoryProxy> ret = new ArrayList<EntityFactoryProxy>();
    try {
      for (Class<?> itm : proxyclses) {
        if (EntityFactoryProxy.class.isAssignableFrom(itm)) {
            ret.add((EntityFactoryProxy)itm.getDeclaredConstructor().newInstance());
        } else {
          throw new ConfigurationException(String.format("%s is not EntityFactoryProxy", itm.getName()));
        }
      }
    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      throw new IllegalArgumentException("Failed to instantiate assigned EntityFactoryProxy classes.", e);
    }
    return ret.toArray(new EntityFactoryProxy[0]);
  }

  /**
   * shift durable parameters.
   *
   * @param gtypes
   *          an array of entity generic types
   *
   * @param factoryproxies
   *          an array of entity factory proxies
   *
   * @param len
   *          the length to shift from start
   *
   * @return the pair of shifted parameters.
   */
  public static Pair<DurableType[], EntityFactoryProxy[]> shiftDurableParams(
      DurableType[] gtypes, EntityFactoryProxy[] factoryproxies, int len) {
    if (0 == len) {
      return Pair.of(gtypes, factoryproxies);
    }
    DurableType[] ret_gtypes = {};
    EntityFactoryProxy[] ret_proxies = {};
    if (null != gtypes && gtypes.length > len) {
      ret_gtypes = Arrays.copyOfRange(gtypes, len, gtypes.length);
    }
    if (null != factoryproxies && factoryproxies.length > len) {
      ret_proxies = Arrays.copyOfRange(factoryproxies, len, factoryproxies.length);
    }
    return Pair.of(ret_gtypes, ret_proxies);
  }

  public static void deleteFileOnly(String pathname) {
    File f = new File(pathname);
    if (f.isFile()) {
      if (!f.delete()) {
        throw new ConfigurationException(String.format("Failure to delete the file %s.", pathname));
      }
    }
  }

  /**
   * get the address of an object
   *
   * @param unsafe
   *         an unsafe object
   * @param o
   *         an object to retrieve its address
   *
   * @return
   *         the address of this object
   */
  @SuppressWarnings({"restriction", "UseOfSunClasses"})
  public static long addressOf(sun.misc.Unsafe unsafe, Object o) {
    Object[] array = new Object[] {o};

    long baseOffset = unsafe.arrayBaseOffset(Object[].class);
    int addressSize = unsafe.addressSize();
    long objectAddress;
    switch (addressSize) {
      case 4:
        objectAddress = unsafe.getInt(array, baseOffset);
        break;
      case 8:
        objectAddress = unsafe.getLong(array, baseOffset);
        break;
      default:
        throw new Error("unsupported address size: " + addressSize);
    }
    return objectAddress;
  }



  /**
   * Gets the address value for the memory that backs a direct byte buffer.
   *
   * @param buffer
   *      A buffer to retrieve its address
   *
   * @return
   *      The system address for the buffers
   */
  public static long getAddressFromDirectByteBuffer(ByteBuffer buffer) {
    try {
      Field addressField = Buffer.class.getDeclaredField("address");
      addressField.setAccessible(true);
      return addressField.getLong(buffer);
    } catch (Exception e) {
      throw new RuntimeException("Unable to address field from ByteBuffer", e);
    }
  }

}
