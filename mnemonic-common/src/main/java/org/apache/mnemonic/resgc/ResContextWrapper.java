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

public class ResContextWrapper<MRES> implements ContextWrapper<MRES> {

    protected MRES m_res = null;
    protected ReclaimContext m_context = null;

    public ResContextWrapper() {
        m_res = null;
        m_context = null;
    }

    public ResContextWrapper(MRES res, ReclaimContext context) {
        m_res = res;
        m_context = context;
    }

    @Override
    public MRES getRes() {
        return m_res;
    }

    @Override
    public void setRes(MRES res) {
        m_res = res;
    }

    @Override
    public ReclaimContext getContext() {
        return m_context;
    }

    @Override
    public void setContext(ReclaimContext rctx) {
        m_context = rctx;
    }

}
