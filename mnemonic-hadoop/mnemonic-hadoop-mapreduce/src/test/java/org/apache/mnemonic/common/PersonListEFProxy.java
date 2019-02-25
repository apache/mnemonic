/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mnemonic.common;


import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.ParameterHolder;
import org.apache.mnemonic.RestorableAllocator;

public class PersonListEFProxy implements EntityFactoryProxy {
  @Override
  public <A extends RestorableAllocator<A>> Person<Long> restore(
      A allocator, EntityFactoryProxy[] factoryproxys,
      DurableType[] gfields, long phandler, boolean autoreclaim) {
    return PersonFactory.restore(allocator, factoryproxys, gfields, phandler, autoreclaim);
  }
  @Override
  public <A extends RestorableAllocator<A>> Person<Long> restore(ParameterHolder<A> ph) {
    return PersonFactory.restore(ph.getAllocator(),
            ph.getEntityFactoryProxies(), ph.getGenericTypes(), ph.getHandler(), ph.getAutoReclaim());
  }
  @Override
  public <A extends RestorableAllocator<A>> Person<Long> create(
      A allocator, EntityFactoryProxy[] factoryproxys,
      DurableType[] gfields, boolean autoreclaim) {
    return PersonFactory.create(allocator, factoryproxys, gfields, autoreclaim);
  }
  @Override
  public <A extends RestorableAllocator<A>> Person<Long> create(ParameterHolder<A> ph) {
    return PersonFactory.create(ph.getAllocator(),
            ph.getEntityFactoryProxies(), ph.getGenericTypes(), ph.getAutoReclaim());
  }

}
