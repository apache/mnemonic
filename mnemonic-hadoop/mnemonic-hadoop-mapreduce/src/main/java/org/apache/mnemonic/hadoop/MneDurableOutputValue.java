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

public class MneDurableOutputValue<V>
    implements Writable {

  protected MneDurableOutputSession<V> m_session;
  protected V m_value;

  public MneDurableOutputValue(MneDurableOutputSession<V> sess) {
    m_session = sess;
  }

  public MneDurableOutputValue(MneDurableOutputSession<V> sess, V value) {
    m_session = sess;
    m_value = value;
  }

  public MneDurableOutputSession<V> getSession() {
    return m_session;
  }

  public MneDurableOutputValue<V> of(V value) {
    m_value = value;
    return this;
  }

  public V getValue() {
    return m_value;
  }
  
  public void post() {
    m_session.post(m_value);
  }

  @Override
  public void write(DataOutput out) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    throw new UnsupportedOperationException();
  }
}
