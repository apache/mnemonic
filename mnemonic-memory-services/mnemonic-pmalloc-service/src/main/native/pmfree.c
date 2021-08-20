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

void __pmalloc_free(struct mdesc *mdp, void *addr) {
	size_t block = BLOCK(addr);

	int fragtype = mdp->mblkinfo[block].inuse.fragtype;
	switch (fragtype) {
	case 0:
		free_blocks(mdp, block);
		break;

	default:
		free_blockfrag(mdp, block, fragtype, addr);
		break;
	}
}

void pmfree(void *md, void *addr) {
	struct mdesc *mdp = (struct mdesc *) md;
	register struct alignlist *l;

	if (addr != NULL) {
		for (l = mdp->aligned_blocks; l != NULL; l = l->next) {
			if (l->alignedaddr == addr) {
				l->alignedaddr = NULL;
				addr = l->unalignedaddr;
				break;
			}
		}
		__pmalloc_free(mdp, addr);
		// if (check_heap_free_info(mdp) < 0) exit(1);
	}

}

