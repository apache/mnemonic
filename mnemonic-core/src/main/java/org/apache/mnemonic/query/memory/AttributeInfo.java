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

import org.apache.mnemonic.DurableType;

/**
 * Represents metadata information about an attribute in a durable query.
 * This class encapsulates details such as the attribute's name, type,
 * sort order, and an associated entity field identifier.
 */
public class AttributeInfo {

    // Name of the attribute
    private String name;

    // Durable type of the attribute, indicating its data type
    private DurableType type;

    // Sort order for the attribute (e.g., ASCENDING or DESCENDING)
    private SortOrder sortOrder;

    // Identifier for the entity field this attribute is associated with
    private long entityFieldId;

    /**
     * Retrieves the name of the attribute.
     *
     * @return the name of the attribute
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the attribute.
     *
     * @param name the new name for the attribute
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Retrieves the sort order for this attribute.
     *
     * @return the sort order (e.g., ASCENDING or DESCENDING)
     */
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    /**
     * Sets the sort order for this attribute.
     *
     * @param sortOrder the sort order to be set
     */
    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * Retrieves the type of the attribute.
     *
     * @return the durable type of the attribute
     */
    public DurableType getType() {
        return type;
    }

    /**
     * Sets the type of the attribute.
     *
     * @param type the durable type to be set
     */
    public void setType(DurableType type) {
        this.type = type;
    }

    /**
     * Retrieves the identifier for the associated entity field.
     *
     * @return the entity field identifier
     */
    public long getEntityFieldId() {
        return entityFieldId;
    }

    /**
     * Sets the identifier for the associated entity field.
     *
     * @param entityFieldId the new identifier for the entity field
     */
    public void setEntityFieldId(long entityFieldId) {
        this.entityFieldId = entityFieldId;
    }
}
