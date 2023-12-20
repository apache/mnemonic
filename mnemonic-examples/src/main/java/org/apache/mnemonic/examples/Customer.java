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

@DurableEntity
public abstract class Customer implements Durable {

    /**
     * This method is called after an instance of Customer is created.
     */
    @Override
    public void initializeAfterCreate() {
        // Initialization logic after creation (if needed)
    }

    /**
     * This method is called after an instance of Customer is restored from durable memory.
     */
    @Override
    public void initializeAfterRestore() {
        // Initialization logic after restoration (if needed)
    }

    /**
     * Setup generic information for the entity.
     *
     * @param efproxies array of entity factory proxies
     * @param gftypes   array of durable types
     */
    @Override
    public void setupGenericInfo(EntityFactoryProxy[] efproxies, DurableType[] gftypes) {
        // Setup generic information (if needed)
    }

    /**
     * Getter method for the 'name' field.
     *
     * @return the name of the customer
     * @throws RetrieveDurableEntityError if an error occurs during entity retrieval
     */
    @DurableGetter
    public abstract String getName() throws RetrieveDurableEntityError;

    /**
     * Setter method for the 'name' field.
     *
     * @param name    the name to set for the customer
     * @param destroy whether to destroy the previous value
     * @throws OutOfHybridMemory         if there is not enough hybrid memory available
     * @throws RetrieveDurableEntityError if an error occurs during entity retrieval
     */
    @DurableSetter
    public abstract void setName(String name, boolean destroy)
            throws OutOfHybridMemory, RetrieveDurableEntityError;

    /**
     * Display information about the customer.
     */
    public void show() {
        System.out.printf("Customer Name: %s \n", getName());
    }
}
