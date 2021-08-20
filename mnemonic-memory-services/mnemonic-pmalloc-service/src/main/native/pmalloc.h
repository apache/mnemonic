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

#ifndef PMALLOC_H
#define PMALLOC_H 1

#include <config.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <string.h>
#include <sys/mman.h>
#include <stdio.h>
#include <assert.h>
#include <stddef.h>
#include <errno.h>
#include <limits.h>
#include <unistd.h>

#define PMALLOC_MIN_POOL_SIZE ((size_t)(1024 * 1024 * 16)) /* min pool size: 16MB */

#define PMALLOC_KEYS		255

extern void *pmopen(const char *fn, void *baseaddr, size_t initial_size);

extern void pmclose(void* md);

extern long pmcapacity(void* md);

extern void * pmalloc(void *, size_t);

extern void * pmalloc_check(void *, size_t, void *);

extern void * pmrealloc(void *, void *, size_t);

extern void * pmrealloc_check(void *, void *, size_t, void *);

extern void * pmcalloc(void *, size_t, size_t);

extern void * pmcalloc_check(void *, size_t, size_t, void *);

extern void pmfree(void *, void *);

extern void pmfree_check(void *, void *, void *);

extern void * pmalign(void *, size_t, size_t);

extern void * pmvalloc(void *, size_t);

extern void pmcheck(void *, void (*)(void *, void *, int));

extern struct mempoolstats pmstats(void *);

extern void * pmalloc_attach(int, void *, size_t);

extern void * pmalloc_detach(void *);

extern int pmalloc_setkey(void *, int, void *);

extern void * pmalloc_getkey(void *, int);

extern int pmalloc_errno(void *);

extern int pmtrace(void);

extern int pmsync(void *, void *, size_t);

extern void * p_addr(void *, void *);

extern void * e_addr(void *, void *);

extern void * b_addr(void *);

#endif  /* PMALLOC_H */
