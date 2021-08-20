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

#include "pminternal.h"

inline void * p_addr(void *md, void *addr) {
	struct mdesc *mdp = (struct mdesc *) md;
	return NULL == addr ? NULL : addr - (size_t) (mdp->mempoolbase);
}

inline void * e_addr(void *md, void *addr) {
	struct mdesc *mdp = (struct mdesc *) md;
	return NULL == addr ? NULL : (size_t) addr + mdp->mempoolbase;
}

inline void * b_addr(void *md) {
	struct mdesc *mdp = (struct mdesc *) md;
	return mdp->mempoolbase;
}
