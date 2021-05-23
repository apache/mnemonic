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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collect resource holders that need to be collected after utilization. It
 * manages to release resource in explicit way and we have change to be notified
 * when its resource is going to be release. In addition, It embedded mechanism
 * to allow resource to be forced release in advance.
 * 
 * @author wg
 *
 * @param <HOLDER>
 *            holder type for any kind of resource.
 * 
 * @param <MRES>
 *            resource type to be holden.
 */
public class ResCollector<HOLDER extends Holder<MRES, HOLDER>, MRES>
        implements Collector<HOLDER, MRES> {

    public static final long WAITRECLAIMTIMEOUT = 800;
    public static final long WAITQUEUETIMEOUT = 300;
    public static final long WAITTERMTIMEOUT = 10000;
    public static final long TERMCHECKTIMEOUT = 20;

    private final ReferenceQueue<HOLDER> refque = new ReferenceQueue<HOLDER>();
    private final Map<Reference<HOLDER>, ContextWrapper<MRES>> refmap =
            new ConcurrentHashMap<Reference<HOLDER>, ContextWrapper<MRES>>();

    private ResReclaim<MRES> m_reclaimer;
    private Thread m_collector;
    private AtomicLong descnt = new AtomicLong(0L);

    private volatile boolean m_stopped = false;

    /**
     * constructor to accept a resource reclaimer that is used to actual its
     * resource when needed.
     *
     * @param reclaimer
     *          a reclaimer object for managed resource reclaim.
     */
    public ResCollector(ResReclaim<MRES> reclaimer) {
        m_reclaimer = reclaimer;
        m_collector = new Thread() {
            public void run() {
                while (!m_stopped) {
                    try {
                        Reference<? extends HOLDER> ref = refque.remove(WAITQUEUETIMEOUT);
                        if (null != ref) {
                            destroyRes(ref);
                            descnt.getAndIncrement();
                        }
                    } catch (InterruptedException ex) {
                        break;
                    }
                }
            }
        };
        m_collector.setDaemon(true);
        m_collector.start();
    }

    @Override
    public void register(HOLDER holder) {
        register(holder, null);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.flowcomputing.commons.resgc.Collector#register(org.flowcomputing.
     * commons.resgc.Holder, java.lang.Object)
     */
    @Override
    public void register(HOLDER holder, ReclaimContext rctx) {
        ContextWrapper<MRES> cw = new ResContextWrapper<MRES>(holder.get(), rctx);
        if (null == holder.getRefId()) {
            PhantomReference<HOLDER> pref = new PhantomReference<HOLDER>(holder,
                    refque);
            refmap.put(pref, cw);
            holder.setCollector(this);
            holder.setRefId(pref);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.flowcomputing.commons.resgc.Collector#unregister(org.flowcomputing.
     * commons.resgc.Holder)
     */
    @Override
    public void unregister(HOLDER holder) {
        if (null != holder.getRefId()) {
            refmap.remove(holder.getRefId());
            holder.setRefId(null);
            holder.setCollector(null);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.flowcomputing.commons.resgc.Collector#isRegistered(org.flowcomputing.
     * commons.resgc.Holder)
     */
    @Override
    public boolean isRegistered(HOLDER holder) {
        return null != holder.getRefId() ? refmap.containsKey(holder.getRefId()) : false;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.flowcomputing.commons.resgc.Collector#containsRef()
     */
    @Override
    public boolean containsRef(Reference<HOLDER> ref) {
        return refmap.containsKey(ref);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.flowcomputing.commons.resgc.Collector#removeRef()
     */
    @Override
    public void removeRef(Reference<HOLDER> ref) {
        refmap.remove(ref);
    }

    /**
     * destroy a specified holden resource.
     *
     * @param ref
     *            a specified reference that is referring a holder
     */
    @Override
    public void destroyRes(Reference<? extends HOLDER> ref) {
        ContextWrapper<MRES> cw = refmap.remove(ref);
        m_reclaimer.reclaim(cw);
    }

    @Override
    public void destroyRes(ContextWrapper<MRES> cw) {
        m_reclaimer.reclaim(cw);
    }

    private void forceGC(long timeout) {
        System.gc();
        try {
            Thread.sleep(timeout);
        } catch (Exception ex) {
        }
    }

    /**
     * wait for reclaim termination.
     *
     * @param timeout
     *            specify a timeout to reclaim
     */
    public void waitReclaimCoolDown(long timeout) {
        do {
            forceGC(timeout);
        } while (0L != descnt.getAndSet(0L));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.flowcomputing.commons.resgc.Collector#close()
     */
    @Override
    public boolean close(long recltmout, long termtmout) {
        boolean ret = true;
        waitReclaimCoolDown(recltmout);
        m_stopped = true;
        long et = System.currentTimeMillis() + termtmout;
        while (m_collector.getState() != Thread.State.TERMINATED) {
            try {
                Thread.sleep(TERMCHECKTIMEOUT);
            } catch (Exception ex) {
            }
            if (System.currentTimeMillis() > et) {
                ret = false;
                break;
            }
        }
        refmap.clear();
        return ret;
    }

    public boolean close() {
        return close(WAITRECLAIMTIMEOUT, WAITTERMTIMEOUT);
    }
}
