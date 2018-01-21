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

package org.apache.mnemonic.examples;

import org.apache.mnemonic.Durable;
import org.apache.mnemonic.DurableEntity;
import org.apache.mnemonic.DurableGetter;
import org.apache.mnemonic.DurableSetter;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.OutOfHybridMemory;
import org.apache.mnemonic.RetrieveDurableEntityError;
import org.apache.mnemonic.collections.DurableSinglyLinkedList;

import java.util.Date;

@DurableEntity
public abstract class Order implements Durable {
  protected transient EntityFactoryProxy[] m_node_efproxies;
  protected transient DurableType[] m_node_gftypes;

  @Override
  public void initializeAfterCreate() {
  }

  @Override
  public void initializeAfterRestore() {
  }

  @Override
  public void setupGenericInfo(EntityFactoryProxy[] efproxies, DurableType[] gftypes) {
    m_node_efproxies = efproxies;
    m_node_gftypes = gftypes;
  }

  @DurableGetter
  public abstract String getId() throws RetrieveDurableEntityError;

  @DurableSetter
  public abstract void setId(String str, boolean destroy)
      throws OutOfHybridMemory, RetrieveDurableEntityError;


  public Date getDate() {
    return new Date(getTimestamp());
  }

  public void setDate(Date date, boolean destroy) {
    setTimestamp(date.getTime(), destroy);
  }

  @DurableGetter
  public abstract Long getTimestamp() throws RetrieveDurableEntityError;

  @DurableSetter
  public abstract void setTimestamp(Long ts, boolean destroy)
      throws OutOfHybridMemory, RetrieveDurableEntityError;

  @DurableGetter
  public abstract Customer getCustomer() throws RetrieveDurableEntityError;

  @DurableSetter
  public abstract void setCustomer(Customer customer, boolean destroy) throws RetrieveDurableEntityError;

  @DurableGetter(Id = 2L, EntityFactoryProxies = "m_node_efproxies", GenericFieldTypes = "m_node_gftypes")
  public abstract DurableSinglyLinkedList<Product> getItems() throws RetrieveDurableEntityError;

  @DurableSetter
  public abstract void setItems(DurableSinglyLinkedList<Product> items, boolean destroy)
      throws RetrieveDurableEntityError;

  public void show() {
    System.out.printf("Order ID: %s \n", getId());
    System.out.printf("Order Date: %s \n", getDate().toString());
    System.out.printf("Order Customer: ");
    getCustomer().show();
    System.out.printf("Order Items: --BEGIN-- \n");
    DurableSinglyLinkedList<Product> prodlist = getItems();
    for (Product prod : prodlist) {
      prod.show();
    }
    System.out.printf("Order Items: -- END -- \n");
  }
}
