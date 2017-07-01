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

public class ParameterHolder<A extends RetrievableAllocator<A>> {

    private A allocator;
    private DurableType[] durableType;
    private EntityFactoryProxy[] entityFactoryProxy;
    private boolean autoReclaim;
    private long handler;


    public ParameterHolder() {
        allocator = null;
        durableType = new DurableType[]{};
        entityFactoryProxy = null;
        autoReclaim = true;
        handler = 0;
    }

    public ParameterHolder(A n, DurableType[] d, EntityFactoryProxy[] e, boolean b, long h) {
        this.allocator = n;
        this.durableType = d;
        this.entityFactoryProxy = e;
        this.autoReclaim = b;
        this.handler = h;
    }

    public void setAllocator(A n) {
        allocator = n;
    }

    public A getAllocator() {
        return allocator;
    }

    public void setEntityFactoryProxies(EntityFactoryProxy[] e) {
        entityFactoryProxy = e;
    }

    public EntityFactoryProxy[] getEntityFactoryProxies() {
        return entityFactoryProxy;
    }

    public void setAutoReclaim(boolean b) {
        autoReclaim = b;
    }

    public boolean getAutoReclaim() {
        return autoReclaim;
    }

    public void setGenericTypes(DurableType[] d) {
        durableType = d;
    }

    public DurableType[] getGenericTypes() {
        return durableType;
    }

    public void setHandler(long h) {
        handler = h;
    }

    public long getHandler() {
        return handler;
    }
}
