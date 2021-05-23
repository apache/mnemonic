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

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.mnemonic.resgc.ReclaimContext;

public class ParameterHolder<A extends RetrievableAllocator<A>> {

    private A allocator;
    private boolean autoReclaim;
    private long handler;
    private MutablePair<DurableType[], EntityFactoryProxy[]> dpt;
    private ReclaimContext rctx;


    public ParameterHolder() {
        this.allocator = null;
        this.autoReclaim = true;
        this.handler = 0;
        this.dpt = new MutablePair<DurableType[], EntityFactoryProxy[]>(new DurableType[]{}, null);
        this.rctx = null;
    }

    public ParameterHolder(A n) {
        this.allocator = n;
        this.autoReclaim = true;
        this.handler = 0;
        this.dpt = new MutablePair<DurableType[], EntityFactoryProxy[]>(new DurableType[]{}, null);
        this.rctx = null;
    }

    public ParameterHolder(A n, DurableType[] d) {
        this.allocator = n;
        this.autoReclaim = true;
        this.handler = 0;
        this.dpt = new MutablePair<DurableType[], EntityFactoryProxy[]>(d, null);
        this.rctx = null;
    }

    public ParameterHolder(A n, DurableType[] d, EntityFactoryProxy[] e) {
        this.allocator = n;
        this.autoReclaim = true;
        this.handler = 0;
        this.dpt = new MutablePair<DurableType[], EntityFactoryProxy[]>(d, e);
        this.rctx = null;
    }

    public ParameterHolder(A n, DurableType[] d, EntityFactoryProxy[] e, boolean b, long h) {
        this.allocator = n;
        this.autoReclaim = b;
        this.handler = h;
        this.dpt = new MutablePair<DurableType[], EntityFactoryProxy[]>(d, e);
        this.rctx = null;
    }

    public ParameterHolder(A n, DurableType[] d, EntityFactoryProxy[] e, boolean b, long h, ReclaimContext rctx) {
        this.allocator = n;
        this.autoReclaim = b;
        this.handler = h;
        this.dpt = new MutablePair<DurableType[], EntityFactoryProxy[]>(d, e);
        this.rctx = rctx;
    }

    public void setAllocator(A n) {
        allocator = n;
    }

    public A getAllocator() {
        return allocator;
    }

    public void setEntityFactoryProxies(EntityFactoryProxy[] e) {
        dpt.setRight(e);
    }

    public EntityFactoryProxy[] getEntityFactoryProxies() {
        return dpt.getRight();
    }

    public void setAutoReclaim(boolean b) {
        autoReclaim = b;
    }

    public boolean getAutoReclaim() {
        return autoReclaim;
    }

    public void setGenericTypes(DurableType[] d) {
        dpt.setLeft(d);
    }

    public DurableType[] getGenericTypes() {
        return dpt.getLeft();
    }

    public void setHandler(long h) {
        handler = h;
    }

    public long getHandler() {
        return handler;
    }

    public void setGenericTypeAndEntityFactoryProxyPair(DurableType[] d, EntityFactoryProxy[] e) {
        dpt.setLeft(d);
        dpt.setRight(e);
    }

    public MutablePair<DurableType[], EntityFactoryProxy[]> getGenericTypeAndEntityFactoryProxyPair() {
        return dpt;
    }

    public void setReclaimContext(ReclaimContext r) {
        rctx = r;
    }

    public ReclaimContext getReclaimContext() {
        return rctx;
    }
}
