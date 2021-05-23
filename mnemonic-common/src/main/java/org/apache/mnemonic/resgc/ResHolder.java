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
 * hold any kind of resource and manages its life cycle.
 * 
 * @author wg
 *
 * @param <T>
 *            a resource type for its holder
 */
public class ResHolder<T, H extends ResHolder<T, H>>
        implements Holder<T, H> {

    protected Collector<H, T> m_collector;
    protected T m_mres;
    protected volatile boolean m_hasres = false;
    protected Reference<H> m_refid;

    /**
     * default constructor.
     *
     */
    public ResHolder() {
        m_hasres = false;
    }

    /**
     * constructor to accept its resource.
     *
     * @param mres
     *          a resource to be managed
     */
    public ResHolder(T mres) {
        m_mres = mres;
        m_hasres = true;
    }

    /* (non-Javadoc)
     * @see org.flowcomputing.commons.resgc.Holder#get()
     */
    @Override
    public T get() {
        return m_mres;
    }

    /* (non-Javadoc)
     * @see org.flowcomputing.commons.resgc.Holder#set(java.lang.Object)
     */
    @Override
    public void set(T mres) {
        m_mres = mres;
        m_hasres = true;
    }

    /* (non-Javadoc)
     * @see org.flowcomputing.commons.resgc.Holder#clear()
     */
    @Override
    public void clear() {
        m_hasres = false;
    }

    /* (non-Javadoc)
     * @see org.flowcomputing.commons.resgc.Holder#hasResource()
     */
    @Override
    public boolean hasResource() {
        return m_hasres;
    }

    /* (non-Javadoc)
     * @see org.flowcomputing.commons.resgc.Holder#setCollector(java.lang.Object)
     */
    @Override
    public void setCollector(Collector<H, T> collector) {
        m_collector = collector;
    }

    /* (non-Javadoc)
     * @see org.flowcomputing.commons.resgc.Holder#setRefId(java.lang.Object)
     */
    @Override
    public void setRefId(Reference<H> rid) {
        m_refid = rid;
    }

    /* (non-Javadoc)
     * @see org.flowcomputing.commons.resgc.Holder#getRefId()
     */
    @Override
    public Reference<H> getRefId() {
        return m_refid;
    }

    /* (non-Javadoc)
     * @see org.flowcomputing.commons.resgc.Holder#autoReclaim()
     */
    @Override
    public boolean autoReclaim() {
        return null != m_collector ? m_collector.containsRef(m_refid) : false;
    }

    /* (non-Javadoc)
     * @see org.flowcomputing.commons.resgc.Holder#cancelReclaim()
     */
    @Override
    public void cancelAutoReclaim() {
        if (null != m_refid && null != m_collector) {
            m_collector.removeRef(m_refid);
            m_refid = null;
        }
    }

    @Override
    public void destroy() {
        destroy(null);
    }

    /* (non-Javadoc)
     * @see org.flowcomputing.commons.resgc.Holder#destroy()
     */
    @Override
    public void destroy(ContextWrapper<T> cw) {
        if (null != m_collector) {
            if (null != m_refid) {
                m_collector.removeRef(m_refid);
                m_refid = null;
            }
            if (hasResource()) {
                m_collector.destroyRes(null == cw ? new ResContextWrapper<T>(m_mres, null) : cw);
                m_hasres = false;
            }
        }
    }

}
