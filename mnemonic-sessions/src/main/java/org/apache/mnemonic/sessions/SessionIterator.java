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

package org.apache.mnemonic.sessions;

import java.util.Iterator;

import org.apache.mnemonic.CloseableIterator;
import org.apache.mnemonic.RestorableAllocator;

/**
 * An interface representing a session iterator.
 *
 * @param <V> the type of elements returned by this iterator
 * @param <A> the type of the allocator
 * @param <T> the type of elements in the source iterator
 * @param <S> the type of state associated with the iterator
 */
public interface SessionIterator<V, A extends RestorableAllocator<A>, T, S>
        extends CloseableIterator<V>, DurableComputable<A> {

    /**
     * Sets the allocator for this session iterator.
     *
     * @param alloc the allocator to be set
     */
    void setAllocator(A alloc);

    /**
     * Sets the handler for this session iterator.
     *
     * @param hdl the handler to be set
     */
    void setHandler(long hdl);

    /**
     * Sets the iterator for this session iterator.
     *
     * @param iter the iterator to be set
     */
    void setIterator(Iterator<V> iter);

    /**
     * Retrieves the iterator associated with this session iterator.
     *
     * @return the iterator associated with this session iterator
     */
    Iterator<V> getIterator();

    /**
     * Sets the source iterator for this session iterator.
     *
     * @param iter the source iterator to be set
     */
    void setSourceIterator(Iterator<T> iter);

    /**
     * Retrieves the source iterator associated with this session iterator.
     *
     * @return the source iterator associated with this session iterator
     */
    Iterator<T> getSourceIterator();

    /**
     * Retrieves the state associated with this session iterator.
     *
     * @return the state associated with this session iterator
     */
    S getState();

    /**
     * Sets the state associated with this session iterator.
     *
     * @param state the state to be set
     */
    void setState(S state);
}

