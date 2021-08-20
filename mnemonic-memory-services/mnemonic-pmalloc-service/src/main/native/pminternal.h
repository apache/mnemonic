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

#ifndef __PMINTERNAL_H
#define __PMINTERNAL_H 1

#include "pmalloc.h"

#define PMALLOC_MAGIC		"pmalloc_bigdata1"
#define PMALLOC_MAGIC_SIZE	32
#define PMALLOC_VERSION		2

#define INT_BIT		(CHAR_BIT * sizeof(int))

#define BLOCKLOG	(INT_BIT > 16 ? 12 : 9)

#define FINAL_FREE_BLOCKS	8

#define MALLOC_SEARCH_START	mdp -> mblkinfosearchindex

typedef union {
	struct {
		int fragtype;
		union {
			struct {
				size_t nfreefrags;
				size_t firstfragidxinlist;
			} frag;
			size_t sizeinblock;
		} info;
	} inuse;
	struct {
		size_t size;
		size_t next;
		size_t prev;
	} free;
} malloc_info;

struct alignlist {
	struct alignlist *next;
	void *alignedaddr;
	void *unalignedaddr;
};

struct list {
	struct list *next;
	struct list *prev;
};

struct mempoolstats {
	size_t bytes_total;
	size_t chunks_used;
	size_t bytes_used;
	size_t chunks_free;
	size_t bytes_free;
};

struct mdesc {
	char magicwords[PMALLOC_MAGIC_SIZE];

	unsigned int headersize;

	unsigned char version;

	unsigned int flags;

	int saved_errno;

	void * (*morespace)(struct mdesc *, ptrdiff_t);

	void (*abortfunc)(void *, void *, int);

	size_t mblkinfosize;

	char *mblkinfobase;

	size_t mblkinfosearchindex;

	size_t mblkinfoidxlimit;

	malloc_info *mblkinfo;

	struct mempoolstats mblkstats;

	struct list fragblockhead[BLOCKLOG];

	struct alignlist *aligned_blocks;

	char *mempoolbase;

	char *watermarkpos;

	char *limitpos;

	int mappingfd;

	void *persistkeys[PMALLOC_KEYS];

};

extern void __pmalloc_free(struct mdesc *, void *);

extern struct mdesc *__pmalloc_default_mdp;

extern void * __pmalloc_map_morespace(struct mdesc *, ptrdiff_t);

extern void * __pmalloc_remap_mempool(struct mdesc *);

extern void * pmalloc_attach(int, void *, size_t);

extern void * pmalloc_detach(void *);

extern void rebase_mdesc_infos(struct mdesc *, void *, void *);

#endif  /* __PMINTERNAL_H */
