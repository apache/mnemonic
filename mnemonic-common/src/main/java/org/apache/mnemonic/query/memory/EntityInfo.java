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
 * The EntityInfo class represents metadata about an entity in the Mnemonic framework.
 * It holds information about the entity's class name, entity name, and its attributes.
 */
public class EntityInfo {

  // The fully qualified class name of the entity.
  private String className;

  // The name of the entity.
  private String entityName;

  // An array of AttributeInfo objects, representing the attributes of the entity.
  private AttributeInfo[] attributeInfo;

  /**
   * Gets the class name of the entity.
   *
   * @return the class name as a String.
   */
  public String getClassName() {
    return className;
  }

  /**
   * Sets the class name of the entity.
   *
   * @param className the class name to set.
   */
  public void setClassName(String className) {
    this.className = className;
  }

  /**
   * Gets the name of the entity.
   *
   * @return the entity name as a String.
   */
  public String getEntityName() {
    return entityName;
  }

  /**
   * Sets the name of the entity.
   *
   * @param entityName the entity name to set.
   */
  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  /**
   * Gets the array of AttributeInfo objects associated with the entity.
   *
   * @return an array of AttributeInfo objects.
   */
  public AttributeInfo[] getAttributeInfo() {
    return attributeInfo;
  }

  /**
   * Sets the array of AttributeInfo objects for the entity.
   *
   * @param attributeInfo an array of AttributeInfo objects to set.
   */
  public void setAttributeInfo(AttributeInfo[] attributeInfo) {
    this.attributeInfo = attributeInfo;
  }
}
