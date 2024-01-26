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

/**
 * Represents an order entity that is durable.
 */
@DurableEntity
public abstract class Order implements Durable {

    // Define transient fields for entity factory proxies and durable types
    protected transient EntityFactoryProxy[] m_node_efproxies;
    protected transient DurableType[] m_node_gftypes;

    /**
     * Initializes the entity after creation.
     */
    @Override
    public void initializeAfterCreate() {
    }

    /**
     * Initializes the entity after restoration.
     */
    @Override
    public void initializeAfterRestore() {
    }

    /**
     * Sets up generic information for the entity.
     *
     * @param efproxies entity factory proxies
     * @param gftypes   durable types
     */
    @Override
    public void setupGenericInfo(EntityFactoryProxy[] efproxies, DurableType[] gftypes) {
        m_node_efproxies = efproxies;
        m_node_gftypes = gftypes;
    }

    /**
     * Gets the ID of the order.
     *
     * @return the ID of the order
     * @throws RetrieveDurableEntityError if unable to retrieve the durable entity
     */
    @DurableGetter
    public abstract String getId() throws RetrieveDurableEntityError;

    /**
     * Sets the ID of the order.
     *
     * @param str     the ID of the order
     * @param destroy specifies whether to destroy the order
     * @throws OutOfHybridMemory          if out of hybrid memory
     * @throws RetrieveDurableEntityError if unable to retrieve the durable entity
     */
    @DurableSetter
    public abstract void setId(String str, boolean destroy)
            throws OutOfHybridMemory, RetrieveDurableEntityError;

    /**
     * Gets the date of the order.
     *
     * @return the date of the order
     */
    public Date getDate() {
        return new Date(getTimestamp());
    }

    /**
     * Sets the date of the order.
     *
     * @param date    the date of the order
     * @param destroy specifies whether to destroy the order
     */
    public void setDate(Date date, boolean destroy) {
        setTimestamp(date.getTime(), destroy);
    }

    /**
     * Gets the timestamp of the order.
     *
     * @return the timestamp of the order
     * @throws RetrieveDurableEntityError if unable to retrieve the durable entity
     */
    @DurableGetter
    public abstract Long getTimestamp() throws RetrieveDurableEntityError;

    /**
     * Sets the timestamp of the order.
     *
     * @param ts      the timestamp of the order
     * @param destroy specifies whether to destroy the order
     * @throws OutOfHybridMemory          if out of hybrid memory
     * @throws RetrieveDurableEntityError if unable to retrieve the durable entity
     */
    @DurableSetter
    public abstract void setTimestamp(Long ts, boolean destroy)
            throws OutOfHybridMemory, RetrieveDurableEntityError;

    /**
     * Gets the customer of the order.
     *
     * @return the customer of the order
     * @throws RetrieveDurableEntityError if unable to retrieve the durable entity
     */
    @DurableGetter
    public abstract Customer getCustomer() throws RetrieveDurableEntityError;

    /**
     * Sets the customer of the order.
     *
     * @param customer the customer of the order
     * @param destroy  specifies whether to destroy the order
     * @throws RetrieveDurableEntityError if unable to retrieve the durable entity
     */
    @DurableSetter
    public abstract void setCustomer(Customer customer, boolean destroy) throws RetrieveDurableEntityError;

    /**
     * Gets the items of the order.
     *
     * @return the items of the order
     * @throws RetrieveDurableEntityError if unable to retrieve the durable entity
     */
    @DurableGetter(Id = 2L, EntityFactoryProxies = "m_node_efproxies", GenericFieldTypes = "m_node_gftypes")
    public abstract DurableSinglyLinkedList<Product> getItems() throws RetrieveDurableEntityError;

    /**
     * Sets the items of the order.
     *
     * @param items   the items of the order
     * @param destroy specifies whether to destroy the order
     * @throws RetrieveDurableEntityError if unable to retrieve the durable entity
     */
    @DurableSetter
    public abstract void setItems(DurableSinglyLinkedList<Product> items, boolean destroy)
            throws RetrieveDurableEntityError;

    /**
     * Shows the details of the order.
     */
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
