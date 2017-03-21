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
import java.util.NoSuchElementException;

import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.RestorableAllocator;

public abstract class DurableInputSession<V, A extends RestorableAllocator<A>>
    implements InputSession<V>, DurableComputable<A> {

  private String serviceName;
  private DurableType[] durableTypes;
  private EntityFactoryProxy[] entityFactoryProxies;
  private long slotKeyId;

  protected long m_handler;
  protected A m_act;
  protected Iterator<V> m_iter;

  /**
   * One session can only manage one iterator instance at a time for the simplicity
   *
   * @return the singleton iterator
   *
   */
  @Override
  public Iterator<V> iterator() {
    return new Intr();
  }

  /**
   * this class defines a iterator for multiple pools read
   *
   */
  private class Intr implements Iterator<V> {

    /**
     * determine the existing of next
     *
     * @return true if there is a next node
     *
     */
    @Override
    public boolean hasNext() {
      if (null == m_iter) {
        return false;
      }
      boolean ret = m_iter.hasNext();
      if (!ret) {
        if (initNextPool()) {
          ret = m_iter.hasNext();
        }
      }
      return ret;
    }

    /**
     * get next node
     *
     * @return the next node
     */
    @Override
    public V next() {
      if (null == m_iter) {
        throw new NoSuchElementException();
      }
      return m_iter.next();
    }

    /**
     * override remove()
     */
    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public void close() {
    if (null != m_act) {
      m_act.close();
    }
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public DurableType[] getDurableTypes() {
    return durableTypes;
  }

  public void setDurableTypes(DurableType[] durableTypes) {
    this.durableTypes = durableTypes;
  }

  public EntityFactoryProxy[] getEntityFactoryProxies() {
    return entityFactoryProxies;
  }

  public void setEntityFactoryProxies(EntityFactoryProxy[] entityFactoryProxies) {
    this.entityFactoryProxies = entityFactoryProxies;
  }

  public long getSlotKeyId() {
    return slotKeyId;
  }

  public void setSlotKeyId(long slotKeyId) {
    this.slotKeyId = slotKeyId;
  }

  @Override
  public A getAllocator() {
    return m_act;
  }

  @Override
  public long getHandler() {
    return m_handler;
  }

}
