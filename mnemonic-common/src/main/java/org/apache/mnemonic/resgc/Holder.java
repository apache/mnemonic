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

package org.apache.mnemonic.resgc;

import java.lang.ref.Reference;

/**
 * A interface that defines a holder. it is used to hold any kind of resource
 * and provide way to access its holden resource. In addition, it should allow
 * its resource to be destroyed manually ahead of GC.
 * 
 * @author wg
 *
 * @param <T> a resource type for its holder 
 */
public interface Holder<T, H extends Holder<T, H>> {

    /**
     * get its holden resource. Note that it should not be used for assignment.
     *
     * @return its holden resource
     */
    T get();

    /**
     * set its holden resource.
     *
     * @param mres
     *            the holder will be set to a specified resource.
     *
     * Note: must unregister this holder before changing its managed
     *       resource.
     */
    void set(T mres);

    /**
     * clear its managed resource
     */
    void clear();

    /**
     * return whether it has managed resource
     *
     * @return if hold its managed resource
     */
    boolean hasResource();

    /**
     * set collector.
     *
     * @param collector
     *            the collector to manage this holder.
     */
    void setCollector(Collector<H, T> collector);

    /**
     * set reference id.
     *
     * @param rid
     *            the reference id to be managed.
     */
    void setRefId(Reference<H> rid);

    /**
     * get its managed reference id.
     *
     * @return its managed reference id
     */
    Reference<H> getRefId();

    /**
     * determine if this holder is enabled to be auto reclaimed.
     *
     * @return true if autoclaim is enabled
     */
    boolean autoReclaim();

    /**
     * prevent resource from being reclaimed.
     *
     */
    void cancelAutoReclaim();

    /**
     * destroy its holden resource and unregister from its collector.
     *
     */
    void destroy();

    void destroy(ContextWrapper<T> cw);
}
