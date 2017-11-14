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

import org.apache.mnemonic.Durable;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.DurableEntity;
import org.apache.mnemonic.DurableGetter;
import org.apache.mnemonic.DurableSetter;

/**
 * this class defines a non-volatile node for a generic value to form a
 * unidirectional link
 *
 */
@DurableEntity
public abstract class SinglyLinkedNode<E> implements Durable {
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
   * get the item value of this node
   *
   * @return the item value of this node
   */
  @DurableGetter(Id = 1L, EntityFactoryProxies = "m_node_efproxies", GenericFieldTypes = "m_node_gftypes")
  public abstract E getItem();

  /**
   * set a value to this node item
   *
   * @param value
   *          the value to be set
   *
   * @param destroy
   *          true if want to destroy exist one
   *
   */
  @DurableSetter
  public abstract void setItem(E value, boolean destroy);

  /**
   * get next node
   *
   * @return the next node
   *
   */
  @DurableGetter(Id = 2L, EntityFactoryProxies = "m_node_efproxies", GenericFieldTypes = "m_node_gftypes")
  public abstract SinglyLinkedNode<E> getNext();

  /**
   * set next node
   *
   * @param next
   *          specify the next node
   *
   * @param destroy
   *          true if want to destroy the exist node
   */
  @DurableSetter
  public abstract void setNext(SinglyLinkedNode<E> next, boolean destroy);

}
