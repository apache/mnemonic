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
package org.apache.mnemonic.query.memory;

/**
 * Enum representing different sort orders that can be applied to a query.
 */
public enum SortOrder {

    // Enum constants representing the sort order types
    NONE(1),        // No sorting
    ASCENDING(2),   // Ascending order sorting
    DESCENDING(3);  // Descending order sorting

    // Private field to store the integer value associated with each sort order
    private int value;

    /**
     * Constructor to initialize the enum constant with a specific integer value.
     * 
     * @param val The integer value representing the sort order.
     */
    SortOrder(int val) {
        this.value = val;
    }

    /**
     * Getter method to retrieve the integer value associated with the sort order.
     * 
     * @return The integer value representing the sort order.
     */
    public int getValue() {
        return value;
    }
}

