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

void rebase_mdesc_infos(struct mdesc * mdp, void *e_addr, void *o_addr) {
	if (e_addr == o_addr)
		return;
	printf("Rebase Address from %p to %p \n", o_addr, e_addr);
	//sleep(5);
	ptrdiff_t off;
	off = e_addr - o_addr;
	assert(mdp != NULL);
	mdp->mempoolbase = e_addr;
	mdp->mblkinfo = REBASE_ADDRESS(mdp->mblkinfo, off);
	mdp->watermarkpos = REBASE_ADDRESS(mdp->watermarkpos, off);
	mdp->limitpos = REBASE_ADDRESS(mdp->limitpos, off);
	mdp->mblkinfobase = REBASE_ADDRESS(mdp->mblkinfobase, off);
	size_t idx;

	/* not necessary to rebase all persistent key pointer since user do it instead more elegantly.
	 for (idx = 0; idx < PMALLOC_KEYS; ++idx) {
	 if (NULL != mdp -> keys[idx]) {
	 mdp -> keys[idx] = REBASE_ADDRESS(mdp -> keys[idx], off);
	 }
	 }
	 */

	struct alignlist **pal = &mdp->aligned_blocks;
	while (NULL != *pal) {
		*pal = REBASE_ADDRESS(*pal, off);
		pal = &(*pal)->next;
	}

	for (idx = 0; idx < BLOCKLOG; ++idx) {
		struct list *plist = &mdp->fragblockhead[idx];
		while (NULL != plist) {
			if (NULL != plist->next) {
				plist->next = REBASE_ADDRESS(plist->next, off);
			}
			if (NULL != plist->prev) {
				plist->prev = REBASE_ADDRESS(plist->prev, off);
			}
			plist = plist->next;
		}
	}
}

struct mdesc * reuse_mempool(int fd) {
	struct mdesc mtemp;
	struct mdesc *mdp = NULL;

	if ((lseek(fd, 0L, SEEK_SET) == 0)
			&& (read(fd, (char *) &mtemp, sizeof(mtemp)) == sizeof(mtemp))
			&& (mtemp.headersize == sizeof(mtemp))
			&& (strcmp(mtemp.magicwords, PMALLOC_MAGIC) == 0)
			&& (mtemp.version <= PMALLOC_VERSION)) {
		mtemp.mappingfd = fd;

		void * remap_base = __pmalloc_remap_mempool(&mtemp);
		if (remap_base != MAP_FAILED) {
			mdp = (struct mdesc *) remap_base;
			rebase_mdesc_infos(mdp, remap_base, mdp->mempoolbase);
			mdp->mappingfd = fd;
			mdp->morespace = __pmalloc_map_morespace;
		}
	}
	return (mdp);
}

void *align(struct mdesc * mdp, size_t size) {
	void * result;
	unsigned long int adj;

	result = mdp->morespace(mdp, size);
	adj = RESIDUAL(result, BLOCKSIZE);
	if (adj != 0) {
		adj = BLOCKSIZE - adj;
		mdp->morespace(mdp, adj);
		result = (char *) result + adj;
	}
	return (result);
}

int initialize(struct mdesc *mdp) {
	mdp->mblkinfosize = INITMPOOLSIZE / BLOCKSIZE;
	mdp->mblkinfo = (malloc_info *) align(mdp,
			mdp->mblkinfosize * sizeof(malloc_info));
	if (mdp->mblkinfo == NULL) {
		return (0);
	}
	memset((void *) mdp->mblkinfo, 0, mdp->mblkinfosize * sizeof(malloc_info));
	mdp->mblkinfo[0].free.size = 0;
	mdp->mblkinfo[0].free.next = mdp->mblkinfo[0].free.prev = 0;
	mdp->mblkinfosearchindex = 0;
	mdp->mblkinfobase = (char *) mdp->mblkinfo;
	mdp->flags |= PMALLOC_INITIALIZED;
	return (1);
}

void *morespace(struct mdesc *mdp, size_t size) {
	void * result;
	malloc_info *newinfo, *oldinfo;
	size_t newsize;

	result = align(mdp, size);
	if (result == NULL) {
		return (NULL);
	}

	if ((size_t) BLOCK((char * ) result + size) > mdp->mblkinfosize) {
		newsize = mdp->mblkinfosize;
		while ((size_t) BLOCK((char * ) result + size) > newsize) {
			newsize *= 2;
		}
		newinfo = (malloc_info *) align(mdp, newsize * sizeof(malloc_info));
		if (newinfo == NULL) {
			mdp->morespace(mdp, -size);
			return (NULL);
		}
		memset((void *) newinfo, 0, newsize * sizeof(malloc_info));
		memcpy((void *) newinfo, (void *) mdp->mblkinfo,
				mdp->mblkinfosize * sizeof(malloc_info));
		oldinfo = mdp->mblkinfo;
		newinfo[BLOCK(oldinfo)].inuse.fragtype = 0;
		newinfo[BLOCK(oldinfo)].inuse.info.sizeinblock = BLOCKIFY(
				mdp->mblkinfosize * sizeof(malloc_info));
		mdp->mblkinfo = newinfo;
		__pmalloc_free(mdp, (void *) oldinfo);
		mdp->mblkinfosize = newsize;
	}

	mdp->mblkinfoidxlimit = BLOCK((char * ) result + size);
	return (result);
}

void *allocate_blockfrag(struct mdesc *mdp, size_t size)
{
    void * result = NULL;
    size_t block;
    register size_t i;
    struct list *next;
    register size_t log;

    log = 1;
    --size;
    while ((size /= 2) != 0)
    {
        ++log;
    }

    next = mdp -> fragblockhead[log].next;
    if (next != NULL)
    {
        result = (void *) next;
        next -> prev -> next = next -> next;
        if (next -> next != NULL)
        {
            next -> next -> prev = next -> prev;
        }
        block = BLOCK (result);
        if (--mdp -> mblkinfo[block].inuse.info.frag.nfreefrags != 0)
        {
            mdp -> mblkinfo[block].inuse.info.frag.firstfragidxinlist =
                RESIDUAL (next -> next, BLOCKSIZE) >> log;
        }

        mdp -> mblkstats.chunks_used++;
        mdp -> mblkstats.bytes_used += 1 << log;
        mdp -> mblkstats.chunks_free--;
        mdp -> mblkstats.bytes_free -= 1 << log;
    }
    else
    {
        result = allocate_blocks (mdp, 1);
        if (result != NULL)
        {
            for (i = 1; i < (size_t) (BLOCKSIZE >> log); ++i)
            {
                next = (struct list *) ((char *) result + (i << log));
                next -> next = mdp -> fragblockhead[log].next;
                next -> prev = &mdp -> fragblockhead[log];
                next -> prev -> next = next;
                if (next -> next != NULL)
                {
                    next -> next -> prev = next;
                }
            }

            block = BLOCK (result);
            mdp -> mblkinfo[block].inuse.fragtype = log;
            mdp -> mblkinfo[block].inuse.info.frag.nfreefrags = i - 1;
            mdp -> mblkinfo[block].inuse.info.frag.firstfragidxinlist = i - 1;

            mdp -> mblkstats.chunks_free += (BLOCKSIZE >> log) - 1;
            mdp -> mblkstats.bytes_free += BLOCKSIZE - (1 << log);
            mdp -> mblkstats.bytes_used -= BLOCKSIZE - (1 << log);
        }
    }

    return result;
}

void *allocate_blocks(struct mdesc *mdp, size_t blocks)
{
    void * result = NULL;
    size_t block, lastblocks, start;
    start = block = MALLOC_SEARCH_START;
    while (mdp -> mblkinfo[block].free.size < blocks)
    {
        block = mdp -> mblkinfo[block].free.next;
        if (block == start)
        {
            block = mdp -> mblkinfo[0].free.prev;
            lastblocks = mdp -> mblkinfo[block].free.size;
            if (mdp -> mblkinfoidxlimit != 0 &&
                    block + lastblocks == mdp -> mblkinfoidxlimit &&
                    mdp -> morespace (mdp, 0) == ADDRESS(block + lastblocks) &&
                    (morespace (mdp, (blocks - lastblocks) * BLOCKSIZE)) != NULL)
            {

                block = mdp -> mblkinfo[0].free.prev;

                mdp -> mblkinfo[block].free.size += (blocks - lastblocks);
                mdp -> mblkstats.bytes_free +=
                    (blocks - lastblocks) * BLOCKSIZE;
                continue;
            }
            result = morespace(mdp, blocks * BLOCKSIZE);
            if (result != NULL)
            {
                block = BLOCK (result);
                mdp -> mblkinfo[block].inuse.fragtype = 0;
                mdp -> mblkinfo[block].inuse.info.sizeinblock = blocks;
                mdp -> mblkstats.chunks_used++;
                mdp -> mblkstats.bytes_used += blocks * BLOCKSIZE;
            }
            return (result);
        }
    }

    result = ADDRESS(block);
    if (mdp -> mblkinfo[block].free.size > blocks)
    {
        mdp -> mblkinfo[block + blocks].free.size
        = mdp -> mblkinfo[block].free.size - blocks;
        mdp -> mblkinfo[block + blocks].free.next
        = mdp -> mblkinfo[block].free.next;
        mdp -> mblkinfo[block + blocks].free.prev
        = mdp -> mblkinfo[block].free.prev;
        mdp -> mblkinfo[mdp -> mblkinfo[block].free.prev].free.next
        = mdp -> mblkinfo[mdp -> mblkinfo[block].free.next].free.prev
          = mdp -> mblkinfosearchindex = block + blocks;
    }
    else
    {
        mdp -> mblkinfo[mdp -> mblkinfo[block].free.next].free.prev
        = mdp -> mblkinfo[block].free.prev;
        mdp -> mblkinfo[mdp -> mblkinfo[block].free.prev].free.next
        = mdp -> mblkinfosearchindex = mdp -> mblkinfo[block].free.next;
        mdp -> mblkstats.chunks_free--;
    }

    mdp -> mblkinfo[block].inuse.fragtype = 0;
    mdp -> mblkinfo[block].inuse.info.sizeinblock = blocks;
    mdp -> mblkstats.chunks_used++;
    mdp -> mblkstats.bytes_used += blocks * BLOCKSIZE;
    mdp -> mblkstats.bytes_free -= blocks * BLOCKSIZE;
    return result;
}

void free_blocks(struct mdesc *mdp, size_t block)
{
    size_t blocks;
    register size_t i;
    /*    struct list *prev, *next; */
    mdp -> mblkstats.chunks_used--;
    mdp -> mblkstats.bytes_used -=
        mdp -> mblkinfo[block].inuse.info.sizeinblock * BLOCKSIZE;
    mdp -> mblkstats.bytes_free +=
        mdp -> mblkinfo[block].inuse.info.sizeinblock * BLOCKSIZE;

    i = mdp -> mblkinfosearchindex;
    if (i > block)
    {
        while (i > block)
        {
            i = mdp -> mblkinfo[i].free.prev;
        }
    }
    else
    {
        do
        {
            i = mdp -> mblkinfo[i].free.next;
        }
        while ((i != 0) && (i < block));
        i = mdp -> mblkinfo[i].free.prev;
    }

    if (block == i + mdp -> mblkinfo[i].free.size)
    {
        mdp -> mblkinfo[i].free.size +=
            mdp -> mblkinfo[block].inuse.info.sizeinblock;
        block = i;
    }
    else
    {
        mdp -> mblkinfo[block].free.size =
            mdp -> mblkinfo[block].inuse.info.sizeinblock;
        mdp -> mblkinfo[block].free.next = mdp -> mblkinfo[i].free.next;
        mdp -> mblkinfo[block].free.prev = i;
        mdp -> mblkinfo[i].free.next = block;
        mdp -> mblkinfo[mdp -> mblkinfo[block].free.next].free.prev = block;
        mdp -> mblkstats.chunks_free++;
    }

    if (block + mdp -> mblkinfo[block].free.size ==
            mdp -> mblkinfo[block].free.next)
    {
        mdp -> mblkinfo[block].free.size
        += mdp -> mblkinfo[mdp -> mblkinfo[block].free.next].free.size;
        mdp -> mblkinfo[block].free.next
        = mdp -> mblkinfo[mdp -> mblkinfo[block].free.next].free.next;
        mdp -> mblkinfo[mdp -> mblkinfo[block].free.next].free.prev = block;
        mdp -> mblkstats.chunks_free--;
    }

    blocks = mdp -> mblkinfo[block].free.size;
    if (blocks >= FINAL_FREE_BLOCKS && block + blocks == mdp -> mblkinfoidxlimit
            && mdp -> morespace (mdp, 0) == ADDRESS (block + blocks))
    {
        register size_t bytes = blocks * BLOCKSIZE;
        mdp -> mblkinfoidxlimit -= blocks;
        mdp -> morespace (mdp, -bytes);
        mdp -> mblkinfo[mdp -> mblkinfo[block].free.prev].free.next
        = mdp -> mblkinfo[block].free.next;
        mdp -> mblkinfo[mdp -> mblkinfo[block].free.next].free.prev
        = mdp -> mblkinfo[block].free.prev;
        block = mdp -> mblkinfo[block].free.prev;
        mdp -> mblkstats.chunks_free--;
        mdp -> mblkstats.bytes_free -= bytes;
    }

    mdp -> mblkinfosearchindex = block;
}

void free_blockfrag(struct mdesc *mdp, size_t block, int fraglog, void *addr)
{
    /* size_t blocks; */
    register size_t i;
    struct list *prev, *next;

    mdp -> mblkstats.chunks_used--;
    mdp -> mblkstats.bytes_used -= 1 << fraglog;
    mdp -> mblkstats.chunks_free++;
    mdp -> mblkstats.bytes_free += 1 << fraglog;

    prev = (struct list *)
           ((char *) ADDRESS(block) +
            (mdp -> mblkinfo[block].inuse.info.frag.firstfragidxinlist << fraglog));

    if (mdp -> mblkinfo[block].inuse.info.frag.nfreefrags ==
            (BLOCKSIZE >> fraglog) - 1)
    {
        next = prev;
        for (i = 1; i < (size_t) (BLOCKSIZE >> fraglog); ++i)
        {
            next = next -> next;
        }
        prev -> prev -> next = next;
        if (next != NULL)
        {
            next -> prev = prev -> prev;
        }
        mdp -> mblkinfo[block].inuse.fragtype = 0;
        mdp -> mblkinfo[block].inuse.info.sizeinblock = 1;

        mdp -> mblkstats.chunks_used++;
        mdp -> mblkstats.bytes_used += BLOCKSIZE;
        mdp -> mblkstats.chunks_free -= BLOCKSIZE >> fraglog;
        mdp -> mblkstats.bytes_free -= BLOCKSIZE;

        pmfree ((void *) mdp, (void *) ADDRESS(block));
    }
    else if (mdp -> mblkinfo[block].inuse.info.frag.nfreefrags != 0)
    {
        next = (struct list *) addr;
        next -> next = prev -> next;
        next -> prev = prev;
        prev -> next = next;
        if (next -> next != NULL)
        {
            next -> next -> prev = next;
        }
        ++mdp -> mblkinfo[block].inuse.info.frag.nfreefrags;
    }
    else
    {
        prev = (struct list *) addr;
        mdp -> mblkinfo[block].inuse.info.frag.nfreefrags = 1;
        mdp -> mblkinfo[block].inuse.info.frag.firstfragidxinlist =
            RESIDUAL (addr, BLOCKSIZE) >> fraglog;
        prev -> next = mdp -> fragblockhead[fraglog].next;
        prev -> prev = &mdp -> fragblockhead[fraglog];
        prev -> prev -> next = prev;
        if (prev -> next != NULL)
        {
            prev -> next -> prev = prev;
        }
    }
}

void *pmopen(const char *fn, void *baseaddr, size_t initial_size)
{
	void *ret = NULL;
	assert(NULL != fn);
	int fd = open(fn, O_CREAT|O_RDWR, S_IRUSR|S_IWUSR);
	if(fd >= 0) {
		ret =  pmalloc_attach(fd, baseaddr, initial_size);
	}
	return ret;
}

long pmcapacity(void* md)
{
	struct mdesc *mdp = (struct mdesc *) md;
	if (NULL == mdp->limitpos || NULL == mdp->mempoolbase ||
	    mdp->limitpos <=  mdp->mempoolbase) {
		return 0L;
	}
	return mdp->limitpos - mdp->mempoolbase;
}

void pmclose(void* md)
{
	struct mdesc *mdp = (struct mdesc *)md;
	assert(NULL != mdp);
	/* pmalloc_detach(md); */
	close(mdp->mappingfd);
}
