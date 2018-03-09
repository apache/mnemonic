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

import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class EntityFactoryProxyHelper<D extends Durable>
    implements EntityFactoryProxy {

  protected Method fcreatemtd, frestoremtd;
  protected int shiftnum;

  public EntityFactoryProxyHelper(Class<D> clazz)
      throws ClassNotFoundException, NoSuchMethodException {
    this(clazz, 0);
  }

  public EntityFactoryProxyHelper(Class<D> clazz, int shiftnum)
      throws ClassNotFoundException, NoSuchMethodException {
    if (shiftnum < 0) {
      throw new OutOfBoundsException("Shift number cannot be negative");
    }
    this.shiftnum = shiftnum;
    Class c = Class.forName(clazz.getName() + "Factory");
    Method[] fmtds = c.getDeclaredMethods();
    for (int i = 0; i < fmtds.length; ++i) {
      if (fmtds[i].getName().equals("create") && fmtds[i].getParameterCount() == 4) {
        fcreatemtd = fmtds[i];
        break;
      }
    }
    if (null == fcreatemtd) {
      throw new NoSuchMethodException("Not found proper factory create(...) method");
    }
    for (int i = 0; i < fmtds.length; ++i) {
      if (fmtds[i].getName().equals("restore") && fmtds[i].getParameterCount() == 5) {
        frestoremtd = fmtds[i];
        break;
      }
    }
    if (null == frestoremtd) {
      throw new NoSuchMethodException("Not found proper factory restore(...) method");
    }
  }

  @Override
  public <A extends RestorableAllocator<A>> D create(
      A allocator, EntityFactoryProxy[] factoryproxys,
      DurableType[] gfields, boolean autoreclaim) {
    if (shiftnum > gfields.length - 1) {
      throw new OutOfBoundsException("Shift number is out of the range of generic fields");
    }
    Pair<DurableType[], EntityFactoryProxy[]> dpt = Utils.shiftDurableParams(gfields, factoryproxys, shiftnum);
    Object o = null;
    try {
      o = fcreatemtd.invoke(null, allocator, dpt.getRight(), dpt.getLeft(), autoreclaim);
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
    return (D)o;
  }

  @Override
  public <A extends RestorableAllocator<A>> D create(
      ParameterHolder<A> ph) {
    if (shiftnum > ph.getGenericTypes().length - 1) {
      throw new OutOfBoundsException("Shift number is out of the range of generic fields");
    }
    Pair<DurableType[], EntityFactoryProxy[]> dpt = Utils.shiftDurableParams(ph.getGenericTypes(),
        ph.getEntityFactoryProxies(), shiftnum);
    Object o = null;
    try {
      o = fcreatemtd.invoke(null, ph.getAllocator(),
          dpt.getRight(), dpt.getLeft(), ph.getAutoReclaim());
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
    return (D)o;
  }

  @Override
  public <A extends RestorableAllocator<A>> D restore(
      A allocator, EntityFactoryProxy[] factoryproxys,
      DurableType[] gfields, long phandler, boolean autoreclaim) {
    if (shiftnum > gfields.length - 1) {
      throw new OutOfBoundsException("Shift number is out of the range of generic fields");
    }
    Pair<DurableType[], EntityFactoryProxy[]> dpt = Utils.shiftDurableParams(gfields, factoryproxys, shiftnum);
    Object o = null;
    try {
      o = frestoremtd.invoke(null, allocator, dpt.getRight(), dpt.getLeft(), phandler, autoreclaim);
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
    return (D)o;
  }

  @Override
  public <A extends RestorableAllocator<A>> D restore(
      ParameterHolder<A> ph) {
    if (shiftnum > ph.getGenericTypes().length - 1) {
      throw new OutOfBoundsException("Shift number is out of the range of generic fields");
    }
    Pair<DurableType[], EntityFactoryProxy[]> dpt = Utils.shiftDurableParams(ph.getGenericTypes(),
        ph.getEntityFactoryProxies(), shiftnum);
    Object o = null;
    try {
      o = frestoremtd.invoke(null, ph.getAllocator(),
          ph.getEntityFactoryProxies(), dpt.getRight(), dpt.getLeft(), ph.getAutoReclaim());
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
    return (D)o;
  }
}
