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

package org.apache.mnemonic.collections;

import java.util.Iterator;

import org.apache.mnemonic.Durable;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.DurableType;

/**
 * this class defines a non-volatile array implementation
 *
 */
public abstract class DurableArray<E> implements Durable, Iterable<E> {
  protected transient EntityFactoryProxy[] m_node_efproxies;
  protected transient DurableType[] m_node_gftypes;
  protected int arraySize = 0;

  public DurableArray(int size) {
    arraySize = size;
  }

  /**
   * creation callback for initialization
   *
   */
  @Override
  public void initializeAfterCreate() {
    // System.out.println("Initializing After Created");
  }

  /**
   * restore callback for initialization
   *
   */
  @Override
  public void initializeAfterRestore() {
    // System.out.println("Initializing After Restored");
  }

  /**
   * this function will be invoked by its factory to setup generic related info
   * to avoid expensive operations from reflection
   *
   * @param efproxies
   *          specify a array of factory to proxy the restoring of its generic
   *          field objects
   *
   * @param gftypes
   *          specify a array of types corresponding to efproxies
   */
  @Override
  public void setupGenericInfo(EntityFactoryProxy[] efproxies, DurableType[] gftypes) {
    m_node_efproxies = efproxies;
    m_node_gftypes = gftypes;
  }

  /**
   * get the item at a given index
   *
   * @param index
   *          the index of the array
   *
   * @return the item value of this node
   */
  public abstract E get(int index);

  /**
   * set a value at a given index
   *
   * @param index
   *          the index of the array
   *
   * @param value
   *          the value to be set
   */
  public abstract void set(int index, E value);

  /**
   * set a value at a given index with destroy flag
   *
   * @param index
   *          the index of the array
   *
   * @param value
   *          the value to be set
   *
   * @param destroy
   *          true if want to destroy exist one
   *
   */
  public abstract void set(int index, E value, boolean destroy);

  /**
   * get an iterator instance of this list
   *
   * @return an iterator of this list
   */
  public abstract Iterator<E> iterator();

  /**
   * get size of the array
   *
   * @return size of the array
   */
  public int getSize() {
    return arraySize;
  }
}
