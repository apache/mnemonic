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

import org.apache.mnemonic.RestorableAllocator;

/**
 * Represents a durable computable entity.
 *
 * @param <A> the type of the allocator
 */
public interface DurableComputable<A extends RestorableAllocator<A>> {

    /**
     * Retrieves the allocator associated with this computable entity.
     *
     * @return the allocator
     */
    A getAllocator();

    /**
     * Retrieves the handler associated with this computable entity.
     *
     * @return the handler
     */
    long getHandler();
}

