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
 * this class defines a non-volatile HashSet implementation
 *
 */
public abstract class DurableHashSet<E> implements Durable, Iterable<E> {
  protected transient EntityFactoryProxy[] m_node_efproxies;
  protected transient DurableType[] m_node_gftypes;

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
   * checks if set contains the specified element
   *
   * @param item
   *          the item to be searched
   *
   * @return true if set contains the element
   */
  public abstract boolean contains(E item);

  /**
   * adds a specific element to the set
   *
   * @param item
   *          the item to be added
   *
   * @return true if set did not already contain the element
   */
  public abstract boolean add(E item);

  /**
   * removes a specific element from the set
   *
   * @param item
   *          the item to be removed
   *
   * @return true if set contained the element
   */
  public abstract boolean remove(E item);

  /**
   * Get the number of elements in the set
   *
   * @return size of the set
   */
  public abstract long getSize();

  /**
   * get an iterator instance of set
   *
   * @return an iterator of set
   */
  public abstract Iterator<E> iterator();

}
