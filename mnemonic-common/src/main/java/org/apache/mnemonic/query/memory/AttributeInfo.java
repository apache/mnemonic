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
 * This class represents information about an attribute, including its name,
 * type, sort order, and entity field ID.
 */
public class AttributeInfo {

  // The name of the attribute
  private String name;

  // The type of the attribute, represented by a DurableType
  private DurableType type;

  // The sort order of the attribute
  private SortOrder sortOrder;

  // The ID of the entity field associated with this attribute
  private long entityFieldId;

  /**
   * Gets the name of the attribute.
   * 
   * @return the name of the attribute
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the attribute.
   * 
   * @param name the new name of the attribute
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the sort order of the attribute.
   * 
   * @return the sort order of the attribute
   */
  public SortOrder getSortOrder() {
    return sortOrder;
  }

  /**
   * Sets the sort order of the attribute.
   * 
   * @param sortOrder the new sort order of the attribute
   */
  public void setSortOrder(SortOrder sortOrder) {
    this.sortOrder = sortOrder;
  }

  /**
   * Gets the type of the attribute.
   * 
   * @return the type of the attribute
   */
  public DurableType getType() {
    return type;
  }

  /**
   * Sets the type of the attribute.
   * 
   * @param type the new type of the attribute
   */
  public void setType(DurableType type) {
    this.type = type;
  }

  /**
   * Gets the entity field ID associated with the attribute.
   * 
   * @return the entity field ID
   */
  public long getEntityFieldId() {
    return entityFieldId;
  }

  /**
   * Sets the entity field ID associated with the attribute.
   * 
   * @param entityFieldId the new entity field ID
   */
  public void setEntityFieldId(long entityFieldId) {
    this.entityFieldId = entityFieldId;
  }
}
