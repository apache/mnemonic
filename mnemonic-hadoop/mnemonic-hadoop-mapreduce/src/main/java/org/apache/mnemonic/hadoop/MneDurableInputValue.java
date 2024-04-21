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

package org.apache.mnemonic.hadoop;

/**
 * Represents a container for durable input values within the Hadoop environment.
 * @param <V> The type of the value stored in the container.
 */
public class MneDurableInputValue<V> {

  // The session associated with this input value
  protected MneDurableInputSession<V> m_session;
  
  // The stored value
  protected V m_value;

  /**
   * Constructs a new MneDurableInputValue with the given session.
   * @param sess The durable input session associated with this value.
   */
  public MneDurableInputValue(MneDurableInputSession<V> sess) {
    m_session = sess;
  }

  /**
   * Constructs a new MneDurableInputValue with the given session and value.
   * @param sess The durable input session associated with this value.
   * @param value The value to be stored in this container.
   */
  public MneDurableInputValue(MneDurableInputSession<V> sess, V value) {
    m_session = sess;
    m_value = value;
  }

  /**
   * Retrieves the session associated with this input value.
   * @return The durable input session associated with this value.
   */
  public MneDurableInputSession<V> getSession() {
    return m_session;
  }

  /**
   * Sets the value of this container.
   * @param value The value to be stored.
   * @return This MneDurableInputValue instance.
   */
  public MneDurableInputValue<V> of(V value) {
    m_value = value;
    return this;
  }

  /**
   * Retrieves the value stored in this container.
   * @return The value stored in this container.
   */
  public V getValue() {
    return m_value;
  }

}

