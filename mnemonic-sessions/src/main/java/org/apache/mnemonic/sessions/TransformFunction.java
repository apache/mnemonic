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
 * Represents a transformation function that transforms a value of type T into a value of type V.
 *
 * @param <V> the type of the transformed value
 * @param <A> the type of the allocator used for object creation
 * @param <T> the type of the input value to be transformed
 */
public interface TransformFunction<V, A extends RestorableAllocator<A>, T> {

    /**
     * Transforms the input value into a value of type V.
     *
     * @param value       the input value to be transformed
     * @param objcreator  an object creator for creating instances of V using the allocator
     * @return the transformed value of type V
     */
    V transform(T value, ObjectCreator<V, A> objcreator);

}

