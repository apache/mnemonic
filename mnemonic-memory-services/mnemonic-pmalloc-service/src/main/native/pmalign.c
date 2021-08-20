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

#include "pmfunction.h"
#include "pminternal.h"

void *pmalign(void *md, size_t alignment, size_t size) {
	void * result;
	unsigned long int adj;
	struct alignlist *ablist;
	struct mdesc *mdp = (struct mdesc *) md;

	if ((result = pmalloc(md, size + alignment - 1)) != NULL) {
		adj = RESIDUAL(result, alignment);
		if (adj != 0) {
			for (ablist = mdp->aligned_blocks; ablist != NULL;
					ablist = ablist->next) {
				if (ablist->alignedaddr == NULL) {
					break;
				}
			}
			if (ablist == NULL) {
				ablist = (struct alignlist *) pmalloc(md,
						sizeof(struct alignlist));
				if (ablist == NULL) {
					pmfree(md, result);
					return (NULL);
				}
				ablist->next = mdp->aligned_blocks;
				mdp->aligned_blocks = ablist;
			}
			ablist->unalignedaddr = result;
			result = ablist->alignedaddr = (char *) result + alignment - adj;
		}
	}
	return (result);
}
