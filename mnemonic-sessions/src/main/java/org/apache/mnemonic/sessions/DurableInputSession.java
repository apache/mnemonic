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

import org.apache.mnemonic.ConfigurationException;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.RestorableAllocator;

public abstract class DurableInputSession<V, A extends RestorableAllocator<A>, T, S>
    implements InputSession<V> {

  private String serviceName;
  private DurableType[] durableTypes;
  private EntityFactoryProxy[] entityFactoryProxies;
  private long slotKeyId;

  private DurableOutputSession outSession;
  private TransformFunction<V, A, T> transformFunction;

  /**
   * Initialize the next pool, must be called before use
   *
   * @return true if success
   */
  protected abstract boolean init(SessionIterator<V, A, T, S> sessiter);

  /**
   * Initialize the next pool, must be called before using pool
   *
   * @return true if success
   */
  protected abstract boolean initNextPool(SessionIterator<V, A, T, S> sessiter);

  /**
   * One session can only manage one iterator instance at a time for the simplicity
   *
   * @return the singleton iterator
   *
   */
  @Override
  public SessionIterator<V, A, T, S> iterator() {
    SessionIterator<V, A, T, S> ret = new Intr();
    if (!init(ret)) {
      throw new ConfigurationException("Input Session Iterator init. failure");
    }
    return ret;
  }

  /**
   * this class defines a iterator for multiple pools read
   *
   */
  private class Intr implements SessionIterator<V, A, T, S> {

    protected long m_handler;
    protected A m_act;
    protected Iterator<V> m_iter;
    protected Iterator<T> m_srciter;
    protected S m_state;

    /**
     * determine the existing of next
     *
     * @return true if there is a next node
     *
     */
    @Override
    public boolean hasNext() {
      boolean ret = false;
      if (null == m_iter && null == m_srciter) {
        if (!initNextPool(this)) {
          close();
          return false;
        }
      }
      if (null != m_iter) {
        ret = m_iter.hasNext();
        if (!ret) {
          if (initNextPool(this)) {
            ret = m_iter.hasNext();
          }
        }
      } else if (null != m_srciter) {
        ret = m_srciter.hasNext();
      }
      if (!ret) {
        close();
      }
      return ret;
    }

    /**
     * get next node
     *
     * @return the next node
     */
    @SuppressWarnings("unchecked")
    @Override
    public V next() {
      V ret;
      if (null != m_iter) {
        ret = m_iter.next();
      } else if (null != m_srciter) {
        ret = (V)transformFunction.transform(m_srciter.next(), outSession);
        if (null != ret) {
          outSession.post(ret);
        }
      } else {
        throw new NoSuchElementException("Input Session lost it's iterator");
      }
      return ret;
    }

    /**
     * override remove()
     */
    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
    public A getAllocator() {
      return m_act;
    }

    @Override
    public long getHandler() {
      return m_handler;
    }

    @Override
    public void setAllocator(A alloc) {
      m_act = alloc;
    }

    @Override
    public void setHandler(long hdl) {
      m_handler = hdl;
    }

    @Override
    public void setIterator(Iterator<V> iter) {
      m_iter = iter;
    }

    @Override
    public void close() {
      if (null != m_act) {
        m_act.close();
        m_act = null;
      }
      if (null != outSession) {
        outSession.close();
        outSession = null;
      }
    }

    @Override
    public Iterator<V> getIterator() {
      return m_iter;
    }

    @Override
    public void setSourceIterator(Iterator<T> iter) {
      m_srciter = iter;
    }

    @Override
    public Iterator<T> getSourceIterator() {
      return m_srciter;
    }

    @Override
    public S getState() {
      return m_state;
    }

    @Override
    public void setState(S state) {
      m_state = state;
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

  public DurableOutputSession getOutSession() {
    return outSession;
  }

  /**
   * the lifecycle of outSession will be managed here.
   *
   * @param outSession
   *           specify a output sessoin object that is used to generate durable objects as output
   */
  public void setOutSession(DurableOutputSession outSession) {
    this.outSession = outSession;
  }

  public TransformFunction<V, A, T> getTransformFunction() {
    return transformFunction;
  }

  public void setTransformFunction(TransformFunction<V, A, T> transformFunction) {
    this.transformFunction = transformFunction;
  }

  public void close() {
  }

}
