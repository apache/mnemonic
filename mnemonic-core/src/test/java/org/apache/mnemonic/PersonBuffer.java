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
public abstract class PersonBuffer<E> implements Durable, Comparable<PersonBuffer<E>> {
  E element;

  @Override
  public void initializeAfterCreate() {
    System.out.println("WLInitializing After Created");
  }

  @Override
  public void initializeAfterRestore() {
    System.out.println("WLInitializing After Restored");
  }

  @Override
  public void setupGenericInfo(EntityFactoryProxy[] efproxies, DurableType[] gftypes) {

  }

  public void testOutput() throws RetrieveDurableEntityError {
/*    System.out.printf("PersonBuffer %s, Age: %d ( %s ), \n \n BufferCapacity: %d \n", getName(), getAge(),
        null == getMother() ? "No Recorded Mother" : "Has Recorded Mother", getNamebuffer().getSize());*/
    System.out.printf("PersonBuffer %s, Age: %d ( %s ), \n \n BufferCapacity: %d \n", getName(), getAge(),
        null == getMother() ? "No Recorded Mother" : "Has Recorded Mother", getAge());
 
  }

  public int compareTo(PersonBuffer<E> anotherPersonBuffer) {
    int ret = 0;
    if (0 == ret) {
      ret = getAge().compareTo(anotherPersonBuffer.getAge());
    }
    if (0 == ret) {
      ret = getName().compareTo(anotherPersonBuffer.getName());
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

/**/
  @DurableGetter
  public abstract MemBufferHolder getNamebuffer();

  @DurableSetter
  public abstract void setNamebuffer(MemBufferHolder mholder, boolean destroy);
/**/
  @DurableGetter
  public abstract PersonBuffer<E> getMother() throws RetrieveDurableEntityError;

  @DurableSetter
  public abstract void setMother(PersonBuffer<E> mother, boolean destroy) throws RetrieveDurableEntityError;

  @DurableGetter
  public abstract PersonBuffer<E> getFather() throws RetrieveDurableEntityError;

  @DurableSetter
  public abstract void setFather(PersonBuffer<E> mother, boolean destroy) throws RetrieveDurableEntityError;
}
