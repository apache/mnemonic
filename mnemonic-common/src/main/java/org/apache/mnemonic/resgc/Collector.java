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
 * A interface of Collector that is used to collect holders who need to be
 * collected after utilization of its resource.
 * 
 * @author wg
 *
 * @param <HOLDER>
 *            holder type for any kind of resource.
 * @param <MRES>
 *            resource type to be holden.
 */
public interface Collector<HOLDER extends Holder<MRES, HOLDER>, MRES> {

    /**
     * Register a holder and its resource to be managed.
     *
     * @param holder
     *            a holder to be registered
     */
    void register(HOLDER holder);

    /**
     * Register a holder and its resource to be managed.
     *
     * @param holder
     *            a holder to be registered
     *
     * @param rctx
     *            a reclaim context
     */
    void register(HOLDER holder, ReclaimContext rctx);

    /**
     * Unregister a managed resource.
     *
     * @param holder
     *            a holder to be unregistered
     */
    void unregister(HOLDER holder);

    /**
     * Determine if a managed resource is registered.
     *
     * @param holder
     *            a holder to determine
     *
     * @return true of registered
     */
    boolean isRegistered(HOLDER holder);

    /**
     * Determine if a ref is contained.
     *
     * @param ref
     *            a ref object to be determined
     *
     * @return true of contained
     */
    boolean containsRef(Reference<HOLDER> ref);

    /**
     * Remove a Ref that cannot be used alone.
     *
     * @param ref
     *            a reference object to be removed
     */
    void removeRef(Reference<HOLDER> ref);

    /**
     * destroy its managed resource
     *
     * @param ref
     *          a referred resource to be destroyed
     */
    void destroyRes(Reference<? extends HOLDER> ref);

    /**
     * destroy the resource
     *
     * @param cw
     *          a context wrapper to be used to destroy contained resource
     */
    void destroyRes(ContextWrapper<MRES> cw);

    /**
     * close this collector.
     *
     * @param recltmout
     *            specify a timeout for reclaiming
     *
     * @param termtmout
     *            specify a timeout for terminating worker thread
     *
     * @return true if closed gracefully otherwise timeout.
     */
    boolean close(long recltmout, long termtmout);

}
