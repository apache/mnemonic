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

static size_t pagesize = 0;

#define PAGE_ALIGN(addr) \
	(void *) (((long)(addr) + pagesize - 1) & ~(pagesize - 1))

void * __pmalloc_map_morespace(struct mdesc *mdp, ptrdiff_t size) {
	void * result = NULL;
        /*	off_t foffset; */
	size_t mapbytes;
	void *moveto;
	void *mapto;
	char buf = 0;

	if (pagesize == 0) {
		pagesize = getpagesize();
	}
	if (size == 0) {
		result = mdp->watermarkpos;
	} else if (size < 0) {
		if (mdp->watermarkpos + size >= mdp->mempoolbase) {
			result = (void *) mdp->watermarkpos;
			mdp->watermarkpos += size;
		}
	} else {
		if (mdp->watermarkpos + size > mdp->limitpos) {
			if (0 == mdp->limitpos) { /*Initial memory pool*/
				assert(mdp->watermarkpos == 0);
				moveto = PAGE_ALIGN(size);
				mapbytes = (size_t) moveto;

				if (!(mdp->flags & (PMALLOC_DEVZERO | PMALLOC_ANON))) {
					lseek(mdp->mappingfd, mapbytes - 1, SEEK_SET);
					if (write(mdp->mappingfd, &buf, 1) != 1)
						return NULL;
				}
				mapto = mmap(NULL, mapbytes, PROT_READ | PROT_WRITE,
				MAP_SHARED, mdp->mappingfd, 0);
				if (mapto != MAP_FAILED) /* initial */
				{
					mdp->mempoolbase = mdp->watermarkpos = result = mapto;
					mdp->watermarkpos += size;
					mdp->limitpos = PAGE_ALIGN(mdp->watermarkpos);
				}
			}
		} else {
			result = (void *) mdp->watermarkpos;
			mdp->watermarkpos += size;
		}
	}
	return (result);
}

void * __pmalloc_remap_mempool(struct mdesc *mdp) {
	void* base;

	base = mmap(mdp->mempoolbase, (size_t) (mdp->limitpos - mdp->mempoolbase),
	PROT_READ | PROT_WRITE, MAP_SHARED, mdp->mappingfd, 0);
	if (base == MAP_FAILED) {
		fprintf(stderr, "Mapping ERROR:(%d) %s \n", errno, strerror(errno));
	}
	return base;
}
