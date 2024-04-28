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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

/**
 * This class represents a durable output value for Mnemonic Protocol in Hadoop.
 * It implements the Writable interface for serialization.
 *
 * @param <V> the type of the value
 */
public class MneDurableOutputValue<V> implements Writable {

    protected MneDurableOutputSession<V> m_session; // The durable output session
    protected V m_value; // The value to be written

    /**
     * Constructs a MneDurableOutputValue with the specified session.
     *
     * @param sess the durable output session
     */
    public MneDurableOutputValue(MneDurableOutputSession<V> sess) {
        m_session = sess;
    }

    /**
     * Constructs a MneDurableOutputValue with the specified session and value.
     *
     * @param sess  the durable output session
     * @param value the value to be written
     */
    public MneDurableOutputValue(MneDurableOutputSession<V> sess, V value) {
        m_session = sess;
        m_value = value;
    }

    /**
     * Gets the durable output session associated with this value.
     *
     * @return the durable output session
     */
    public MneDurableOutputSession<V> getSession() {
        return m_session;
    }

    /**
     * Sets the value of this output value.
     *
     * @param value the value to be set
     * @return this output value
     */
    public MneDurableOutputValue<V> of(V value) {
        m_value = value;
        return this;
    }

    /**
     * Gets the value of this output value.
     *
     * @return the value
     */
    public V getValue() {
        return m_value;
    }

    /**
     * Posts the value to the associated durable output session.
     */
    public void post() {
        m_session.post(m_value);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        throw new UnsupportedOperationException("Serialization is not supported for MneDurableOutputValue");
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        throw new UnsupportedOperationException("Deserialization is not supported for MneDurableOutputValue");
    }
}

