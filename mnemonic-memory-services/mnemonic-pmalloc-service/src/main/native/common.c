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

inline void *
pmalrealloc(PMALPool *pool, void *p, size_t size, int initzero) {
  void *ret = NULL;
  void *nativebuf = NULL;
  if (NULL == p) {
    if (initzero) {
      nativebuf = pmcalloc(pool->pmp, 1, sizeof(uint8_t) * size + PMBHSZ);
    } else {
      nativebuf = pmalloc(pool->pmp, sizeof(uint8_t) * size + PMBHSZ);
    }
  } else {
    nativebuf = pmrealloc(pool->pmp, p, sizeof(uint8_t) * size + PMBHSZ);
  }
  if (NULL != nativebuf) {
    ((PMBHeader *) nativebuf)->size = size + PMBHSZ;
    ret = nativebuf + PMBHSZ;
  }
  return ret;
}

inline void
pmalfree(PMALPool *pool, void *p) {
  if (p != NULL) {
    pmfree(pool->pmp, p);
  }
}

inline size_t
pmalsize(PMALPool *pool, void *p) {
  size_t ret = 0;
  void* nativebuf;
  if (p != NULL) {
    nativebuf = p - PMBHSZ;
    ret = ((PMBHeader *) nativebuf)->size - PMBHSZ;
  }
  return ret;
}
