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
import org.apache.mnemonic.DurableEntity;
import org.apache.mnemonic.DurableGetter;
import org.apache.mnemonic.DurableSetter;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.OutOfHybridMemory;
import org.apache.mnemonic.RetrieveDurableEntityError;

/**
 * This class defines a non-volatile string that can be stored in a durable
 * (non-volatile) memory.
 */
@DurableEntity
public abstract class DurableString implements Durable, Comparable<DurableString> {

  /**
   * Called after the creation of the object.
   * Can be used for initialization logic.
   */
  @Override
  public void initializeAfterCreate() {
    // Initialization logic after creation
  }

  /**
   * Called after the object is restored from durable memory.
   * Can be used for restoration logic.
   */
  @Override
  public void initializeAfterRestore() {
    // Initialization logic after restoration
  }

  /**
   * Sets up generic information for the entity.
   *
   * @param efproxies the array of entity factory proxies
   * @param gftypes the array of generic field types
   */
  @Override
  public void setupGenericInfo(EntityFactoryProxy[] efproxies, DurableType[] gftypes) {
    // Set up generic info
  }

  /**
   * Compares this DurableString to another DurableString.
   *
   * @param anotherString the other DurableString to compare to
   * @return a negative integer, zero, or a positive integer as this object is less
   *         than, equal to, or greater than the specified object
   */
  @Override
  public int compareTo(DurableString anotherString) {
    return getStr().compareTo(anotherString.getStr());
  }

  /**
   * Gets the string value of this DurableString.
   *
   * @return the string value
   * @throws RetrieveDurableEntityError if an error occurs while retrieving the string
   */
  @DurableGetter
  public abstract String getStr() throws RetrieveDurableEntityError;

  /**
   * Sets the string value of this DurableString.
   *
   * @param str the string value to set
   * @param destroy whether to destroy the old value
   * @throws OutOfHybridMemory if there is not enough memory to store the string
   * @throws RetrieveDurableEntityError if an error occurs while retrieving the string
   */
  @DurableSetter
  public abstract void setStr(String str, boolean destroy) throws OutOfHybridMemory, RetrieveDurableEntityError;

  /**
   * Checks if this DurableString is equal to another object.
   * 
   * @param obj the object to compare to
   * @return true if the objects are equal, false otherwise
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    DurableString that = (DurableString) obj;
    return getStr().equals(that.getStr());
  }

  /**
   * Returns a hash code value for this object.
   * 
   * @return a hash code value for this object
   */
  @Override
  public int hashCode() {
    return getStr().hashCode();
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object
   */
  @Override
  public String toString() {
    return getStr();
  }
}
