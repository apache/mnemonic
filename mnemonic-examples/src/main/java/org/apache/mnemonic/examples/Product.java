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

/**
 * The Product class represents a durable entity that can be persisted
 * and restored from non-volatile memory. It implements the Durable interface,
 * indicating it supports mnemonic protocol features.
 */
@DurableEntity
public abstract class Product implements Durable {

  /**
   * This method is called after the entity is created. It can be used to perform
   * any initialization logic required after creation. Currently, it is empty.
   */
  @Override
  public void initializeAfterCreate() {
  }

  /**
   * This method is called after the entity is restored. It can be used to perform
   * any initialization logic required after restoration. Currently, it is empty.
   */
  @Override
  public void initializeAfterRestore() {
  }

  /**
   * This method sets up the generic information for the entity. It is used to 
   * associate entity factory proxies and durable types with the entity.
   * 
   * @param efproxies Array of entity factory proxies
   * @param gftypes Array of durable types
   */
  @Override
  public void setupGenericInfo(EntityFactoryProxy[] efproxies, DurableType[] gftypes) {
    // Setup logic for generic information can be added here if needed
  }

  /**
   * Retrieves the name of the product.
   * 
   * @return the name of the product
   * @throws RetrieveDurableEntityError if an error occurs while retrieving the entity
   */
  @DurableGetter
  public abstract String getName() throws RetrieveDurableEntityError;

  /**
   * Sets the name of the product.
   * 
   * @param name the name to set
   * @param destroy whether to destroy the previous value
   * @throws OutOfHybridMemory if there is insufficient memory to store the new value
   * @throws RetrieveDurableEntityError if an error occurs while retrieving the entity
   */
  @DurableSetter
  public abstract void setName(String name, boolean destroy)
      throws OutOfHybridMemory, RetrieveDurableEntityError;

  /**
   * Retrieves the price of the product.
   * 
   * @return the price of the product
   * @throws RetrieveDurableEntityError if an error occurs while retrieving the entity
   */
  @DurableGetter
  public abstract Double getPrice() throws RetrieveDurableEntityError;

  /**
   * Sets the price of the product.
   * 
   * @param price the price to set
   * @param destroy whether to destroy the previous value
   * @throws OutOfHybridMemory if there is insufficient memory to store the new value
   * @throws RetrieveDurableEntityError if an error occurs while retrieving the entity
   */
  @DurableSetter
  public abstract void setPrice(Double price, boolean destroy)
      throws OutOfHybridMemory, RetrieveDurableEntityError;

  /**
   * Displays the product's name and price in a formatted string.
   * This method is for demonstration purposes.
   */
  public void show() {
    System.out.printf("%s $%.2f \n", getName(), getPrice());
  }
}
