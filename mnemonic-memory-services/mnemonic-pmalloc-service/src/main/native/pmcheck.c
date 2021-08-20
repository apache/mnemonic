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

extern void abort(void);

#define MAGICWORD	(unsigned int) 0xd4c2afe9
#define MAGICWORDFREE	(unsigned int) 0xff04feca
#define MAGICBYTE	((char) 0xd7)

struct hdr {
	union {
		void *desc;
		char pad[8];
	} u;
	unsigned int size;
	unsigned int magic;
};

static void checkhdr(struct mdesc *mdp, const struct hdr *hdr) {
	if (hdr->magic == MAGICWORDFREE) {
		(*mdp->abortfunc)((void*) (&hdr[1]), hdr->u.desc, -1);
	} else if (hdr->magic != MAGICWORD) {
		(*mdp->abortfunc)((void*) (&hdr[1]), hdr->u.desc, 0);
	} else if (((char*) &hdr[1])[hdr->size] != MAGICBYTE) {
		(*mdp->abortfunc)((void *) (&hdr[1]), hdr->u.desc, 1);
	}
}

void pmfree_check(void * md, void *ptr, void *desc) {
	struct hdr *hdr = ((struct hdr *) ptr) - 1;
	struct mdesc *mdp = (struct mdesc *) md;

	checkhdr(mdp, hdr);
	hdr->magic = MAGICWORDFREE;
	pmfree(md, (void *) hdr);
}

void *pmalloc_check(void *md, size_t size, void *desc) {
	struct hdr *hdr;
        /*	struct mdesc *mdp = (struct mdesc *) md; */
	size_t nbytes;

	nbytes = sizeof(struct hdr) + size + 1;
	hdr = (struct hdr *) pmalloc(md, nbytes);
	if (hdr != NULL) {
		hdr->size = size;
		hdr->u.desc = desc;
		hdr->magic = MAGICWORD;
		hdr++;
		*((char *) hdr + size) = MAGICBYTE;
	}
	return ((void *) hdr);
}

void *pmcalloc_check(void *md, size_t num, size_t size, void *desc) {
	register void * result;
	if ((result = pmalloc_check(md, num * size, desc)) != NULL) {
		bzero(result, num * size);
	}
	return (result);
}

void *pmrealloc_check(void *md, void *ptr, size_t size, void *desc) {
	struct hdr *hdr;
	struct mdesc *mdp = (struct mdesc *) md;
	size_t nbytes;

	if (ptr == NULL)
		return pmalloc_check(md, size, desc);
	hdr = ((struct hdr *) ptr) - 1;
	checkhdr(mdp, hdr);
	nbytes = sizeof(struct hdr) + size + 1;
	hdr = (struct hdr *) pmrealloc(md, (void *) hdr, nbytes);
	if (hdr != NULL) {
		hdr->size = size;
		hdr->u.desc = desc;
		hdr++;
		*((char *) hdr + size) = MAGICBYTE;
	}
	return ((void *) hdr);
}

static void default_abort(void *p, void *desc, int overflow) {
	abort();
}

void pmcheck(void *md, void (*func)(void *, void *, int)) {
	struct mdesc *mdp = (struct mdesc *) md;

	mdp->abortfunc = (func != NULL ? func : default_abort);
	mdp->flags |= PMALLOC_PMCHECK_USED;
}

int check_heap_free_info(struct mdesc *md) {
	int ret = 0;
	size_t start, current, next;
	struct mdesc *mdp = md;
	fprintf(stderr, "\n");

	start = current = MALLOC_SEARCH_START;
	do {
		next = mdp->mblkinfo[current].free.next;

		if (mdp->mblkinfo[next].free.prev != current) {
			fprintf(stderr, "CURRENT->NEXT->prev IS NOT CURRENT !!! \n");
			ret = -1;
			break;
		}

		if (next == start)
			break;

		current = next;

		fprintf(stderr, ".");
	} while (1);

	fprintf(stderr, "\n");
	return ret;
}

