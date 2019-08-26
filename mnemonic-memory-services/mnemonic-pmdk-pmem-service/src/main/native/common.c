/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <common.h>

/******************************************************************************
 ** Generally-useful functions for JNI programming.
 *****************************************************************************/

/**
 *  Throws a RuntimeException, with either an explicit message or the message
 *  corresponding to the current system error value.
 */
inline void
throw(JNIEnv* env, const char* msg) {
  if (msg == NULL)
    msg = strerror(errno);

  jclass xklass = (*env)->FindClass(env, "java/lang/RuntimeException");
  (*env)->ThrowNew(env, xklass, msg);
}

inline void*
addr_from_java(jlong addr) {
  // This assert fails in a variety of ways on 32-bit systems.
  // It is impossible to predict whether native code that converts
  // pointers to longs will sign-extend or zero-extend the addresses.
  //assert(addr == (uintptr_t)addr, "must not be odd high bits");
  return (void*) (uintptr_t) addr;
}

inline jlong
addr_to_java(void* p) {
  assert(p == (void*) (uintptr_t) p);
  return (long) (uintptr_t) p;
}

inline PMEMoid
pmemobj_undirect(PMPool *pool, void *p)
{
  PMEMoid ret = {0, 0};
  if (NULL != pool && NULL != p && (void*)(pool->base) < p) {
    if (pmemobj_pool_by_ptr(p) == pool->pop) {
      ret.pool_uuid_lo = pool->uuid_lo;
      ret.off = (uint64_t)p - (uint64_t)(pool->base);
    }
  }
  return ret;
}

inline void *
prealloc(PMPool *pool, void *p, size_t size, int initzero) {
  void *ret = NULL;
  TOID(uint8_t) m;
  void *nativebuf = NULL;
  if (size > PMEMOBJ_MAX_ALLOC_SIZE) {
    return NULL;
  }
  if (NULL == p) {
    if (initzero) {
      POBJ_ZALLOC(pool->pop, &m.oid, uint8_t, sizeof(uint8_t) * size + PMBHSZ);
    } else {
      POBJ_ALLOC(pool->pop, &m.oid, uint8_t, sizeof(uint8_t) * size + PMBHSZ, NULL, NULL);
    }
  } else {
    m.oid = pmemobj_undirect(pool, p - PMBHSZ);
    if (!TOID_IS_NULL(m)) {
      if (initzero) {
        POBJ_ZREALLOC(pool->pop, &m.oid, uint8_t, sizeof(uint8_t) * size + PMBHSZ);
      } else {
        POBJ_REALLOC(pool->pop, &m.oid, uint8_t, sizeof(uint8_t) * size + PMBHSZ);
      }
    }
  }
  if (!TOID_IS_NULL(m)) {
    nativebuf = pmemobj_direct(m.oid);
    if (nativebuf != NULL) {
      ((PMBHeader *) nativebuf)->size = size + PMBHSZ;
      ret = nativebuf + PMBHSZ;
    }
  }
  return ret;
}

void pfree(PMPool *pool, void *p) {
  PMEMoid oid;
  if (p != NULL) {
    oid = pmemobj_undirect(pool, p - PMBHSZ);
    POBJ_FREE(&oid);
  }
}
