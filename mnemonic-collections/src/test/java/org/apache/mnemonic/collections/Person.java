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

import java.util.Objects;

import org.apache.mnemonic.Durable;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.DurableEntity;
import org.apache.mnemonic.DurableGetter;
import org.apache.mnemonic.DurableSetter;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.OutOfHybridMemory;
import org.apache.mnemonic.RetrieveDurableEntityError;
import org.testng.annotations.Test;

/**
 *
 *
 */

@DurableEntity
public abstract class Person<E> implements Durable, Comparable<Person<E>> {
  E element;

  @Override
  public void initializeAfterCreate() {
    //System.out.println("Initializing After Created");
  }

  @Override
  public void initializeAfterRestore() {
    //System.out.println("Initializing After Restored");
  }

  @Override
  public void setupGenericInfo(EntityFactoryProxy[] efproxies, DurableType[] gftypes) {

  }

  @Test
  public void testOutput() throws RetrieveDurableEntityError {
    System.out.printf("Person %s, Age: %d ( %s ) \n", getName(), getAge(),
        null == getMother() ? "No Recorded Mother" : "Has Recorded Mother");
  }

  public int compareTo(Person<E> anotherPerson) {
    int ret = 0;
    if (0 == ret) {
      ret = getAge().compareTo(anotherPerson.getAge());
    }
    if (0 == ret) {
      ret = getName().compareTo(anotherPerson.getName());
    }
    return ret;
  }

  public int hashCode() {
    return Objects.hash(getAge(), getName());
  }

  @Override
  public boolean equals(Object anotherPerson) {
    return (0 == this.compareTo((Person<E>)anotherPerson)) ? true : false; 
  }
  @DurableGetter(Id = 1L)
  public abstract Short getAge();

  @DurableSetter
  public abstract void setAge(Short age);

  @DurableGetter(Id = 2L)
  public abstract String getName() throws RetrieveDurableEntityError;

  @DurableSetter
  public abstract void setName(String name, boolean destroy)
      throws OutOfHybridMemory, RetrieveDurableEntityError;

  @DurableGetter(Id = 3L)
  public abstract Person<E> getMother() throws RetrieveDurableEntityError;

  @DurableSetter
  public abstract void setMother(Person<E> mother, boolean destroy) throws RetrieveDurableEntityError;

  @DurableGetter(Id = 4L)
  public abstract Person<E> getFather() throws RetrieveDurableEntityError;

  @DurableSetter
  public abstract void setFather(Person<E> mother, boolean destroy) throws RetrieveDurableEntityError;
}
