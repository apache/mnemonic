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

void *pmrealloc(void *md, void *ptr, size_t size) {
	struct mdesc *mdp = (struct mdesc *) md;
	void * result;
	int type;
	size_t block, blocks, oldlimit;

	if (size == 0) {
		if (ptr == NULL)
			return NULL;
		pmfree(md, ptr);
		return (pmalloc(md, 0));
	} else if (ptr == NULL) {
		return (pmalloc(md, size));
	}

	block = BLOCK(ptr);

	type = mdp->mblkinfo[block].inuse.fragtype;
	switch (type) {
	case 0:
		if (size <= BLOCKSIZE / 2) {
			result = pmalloc(md, size);
			if (result != NULL) {
				memcpy(result, ptr, size);
				pmfree(md, ptr);
				return (result);
			}
		}

		blocks = BLOCKIFY(size);
		if (blocks < mdp->mblkinfo[block].inuse.info.sizeinblock) {
			mdp->mblkinfo[block + blocks].inuse.fragtype = 0;
			mdp->mblkinfo[block + blocks].inuse.info.sizeinblock =
					mdp->mblkinfo[block].inuse.info.sizeinblock - blocks;
			mdp->mblkinfo[block].inuse.info.sizeinblock = blocks;
			pmfree(md, ADDRESS(block + blocks));
			result = ptr;
		} else if (blocks == mdp->mblkinfo[block].inuse.info.sizeinblock) {
			result = ptr;
		} else {
			blocks = mdp->mblkinfo[block].inuse.info.sizeinblock;
			oldlimit = mdp->mblkinfoidxlimit;
			mdp->mblkinfoidxlimit = 0;
			pmfree(md, ptr);
			mdp->mblkinfoidxlimit = oldlimit;
			result = pmalloc(md, size);
			if (result == NULL) {
				pmalloc(md, blocks * BLOCKSIZE);
				return (NULL);
			}
			if (ptr != result) {
				memmove(result, ptr, blocks * BLOCKSIZE);
			}
		}
		break;

	default:
		if (size > (size_t) (1 << (type - 1)) && size <= (size_t) (1 << type)) {
			result = ptr;
		} else {
			result = pmalloc(md, size);
			if (result == NULL) {
				return (NULL);
			}
			memcpy(result, ptr, MIN(size, (size_t ) 1 << type));
			pmfree(md, ptr);
		}
		break;
	}

	return (result);
}

