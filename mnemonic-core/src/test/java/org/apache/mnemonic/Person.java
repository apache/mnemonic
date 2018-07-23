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

package org.apache.mnemonic;

/**
 *
 *
 */

@DurableEntity
public abstract class Person<E> implements Durable, Comparable<Person<E>> {
  E element;

  @Override
  public void initializeAfterCreate() {
    System.out.println("Initializing After Created");
  }

  @Override
  public void initializeAfterRestore() {
    System.out.println("Initializing After Restored");
  }

  @Override
  public void setupGenericInfo(EntityFactoryProxy[] efproxies, DurableType[] gftypes) {

  }

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

  @DurableGetter
  public abstract Short getAge();

  @DurableSetter
  public abstract void setAge(Short age);

  @DurableGetter
  public abstract String getName() throws RetrieveDurableEntityError;

  @DurableSetter
  public abstract void setName(String name, boolean destroy)
      throws OutOfHybridMemory, RetrieveDurableEntityError;

  @DurableGetter
  public abstract DurableBuffer<?> getPicture();

  @DurableSetter
  public abstract void setPicture(DurableBuffer<?> mbh, boolean destroy);

  @DurableGetter
  public abstract DurableChunk<?> getPreference();

  @DurableSetter
  public abstract void setPreference(DurableChunk<?> mch, boolean destroy);

  @DurableGetter
  @RefBreak
  public abstract Person<E> getMother() throws RetrieveDurableEntityError;

  @DurableSetter
  public abstract void setMother(Person<E> mother, boolean destroy) throws RetrieveDurableEntityError;

  @DurableGetter
  @RefBreak
  public abstract Person<E> getFather() throws RetrieveDurableEntityError;

  @DurableSetter
  public abstract void setFather(Person<E> father, boolean destroy) throws RetrieveDurableEntityError;
}
