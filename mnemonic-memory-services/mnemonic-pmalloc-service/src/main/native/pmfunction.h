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

#ifndef PMFUNCTION_H
#define PMFUNCTION_H 1

#include "config.h"
#include "pminternal.h"

#define PMALLOC_DEVZERO		(1 << 0)

#define PMALLOC_INITIALIZED	(1 << 1)

#define PMALLOC_PMCHECK_USED	(1 << 2)

#define PMALLOC_ANON		(1 << 3)

#define PMALLOC_FILE		(1 << 4)

#define REBASE_ADDRESS(A, O) ((void*)(A) + (O))

#ifndef MIN
#  define MIN(A, B) ((A) < (B) ? (A) : (B))
#endif

#define BLOCKSIZE	((unsigned int) 1 << BLOCKLOG)
#define BLOCKIFY(SIZE)	(((SIZE) + BLOCKSIZE - 1) / BLOCKSIZE)

#define RESIDUAL(addr, bsize) ((size_t) ((size_t)addr % (bsize)))

#define BLOCK(A) (((char *) (A) - mdp -> mblkinfobase) / BLOCKSIZE + 1)

#define ADDRESS(B) ((void *) (((B) - 1) * BLOCKSIZE + mdp -> mblkinfobase))

#ifndef HAVE_MEMMOVE
#  undef  memmove
#  define memmove(dst,src,len) bcopy(src,dst,len)
#endif

#define INITMPOOLSIZE	(INT_BIT > 16 ? 4 * 1024 * 1024 : 64 * 1024)

void rebase_mdesc_infos(struct mdesc * mdp, void *e_addr, void *o_addr);

struct mdesc *reuse_mempool(int);

int initialize(struct mdesc *);

void *morespace(struct mdesc *, size_t);

void *align(struct mdesc *, size_t);

void *allocate_blockfrag(struct mdesc *mdp, size_t size);

void *allocate_blocks(struct mdesc *mdp, size_t blocks);

void free_blocks(struct mdesc *mdp, size_t block);

void free_blockfrag(struct mdesc *mdp, size_t block, int fraglog, void *addr);

#endif  /* PMFUNCTION_H */
