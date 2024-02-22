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

import java.io.Closeable;

import org.apache.mnemonic.RestorableAllocator;

/**
 * Interface representing an output session for storing data in Mnemonic.
 *
 * @param <V> the type of data to be stored
 * @param <A> the type of allocator used for memory allocation
 */
public interface OutputSession<V, A extends RestorableAllocator<A>>
    extends ObjectCreator<V, A>, DurableComputable<A>, Closeable {

  /**
   * Posts a value to the output session for storage.
   *
   * @param v the value to be stored
   */
  void post(V v);

  /**
   * Destroys a pending record with the specified key.
   *
   * @param k the key of the pending record to be destroyed
   */
  void destroyPendingRecord(V k);

  /**
   * Destroys all pending records in the output session.
   */
  void destroyAllPendingRecords();

}

