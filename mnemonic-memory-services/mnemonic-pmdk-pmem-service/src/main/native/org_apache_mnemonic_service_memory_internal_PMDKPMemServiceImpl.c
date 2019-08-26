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

#include "org_apache_mnemonic_service_memory_internal_PMDKPMemServiceImpl.h"
#include "libpmem.h"

static PMPool *g_pmpool_arr = NULL;
static size_t g_pmpool_count = 0;

static pthread_rwlock_t g_pmp_rwlock = PTHREAD_RWLOCK_INITIALIZER;

/******************************************************************************
 ** JNI implementations
 *****************************************************************************/

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_PMDKPMemServiceImpl_nallocate(JNIEnv* env,
    jobject this, jlong id, jlong size, jboolean initzero) {
  PMPool *pool;
  jlong ret = 0L;
  void *p;
  pthread_rwlock_rdlock(&g_pmp_rwlock);
  pool = g_pmpool_arr + id;
  p = prealloc(pool, NULL, size, initzero ? 1 : 0);
  if (NULL != p) {
    ret = addr_to_java(p);
  }
  pthread_rwlock_unlock(&g_pmp_rwlock);
  return ret;
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_PMDKPMemServiceImpl_nreallocate(JNIEnv* env,
    jobject this, jlong id, jlong addr, jlong size, jboolean initzero) {
  PMPool *pool;
  jlong ret = 0L;
  void *p;
  pthread_rwlock_rdlock(&g_pmp_rwlock);
  pool = g_pmpool_arr + id;
  p = addr_from_java(addr);
  p = prealloc(pool, p, size, initzero ? 1 : 0);
  if (NULL != p) {
    ret = addr_to_java(p);
  }
  pthread_rwlock_unlock(&g_pmp_rwlock);
  return ret;
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_PMDKPMemServiceImpl_nfree(
    JNIEnv* env,
    jobject this, jlong id,
    jlong addr)
{
  PMPool *pool;
  void* nativebuf;
  pthread_rwlock_rdlock(&g_pmp_rwlock);
  pool = g_pmpool_arr + id;
  //fprintf(stderr, "nfree Get Called %ld, %X\n", id, addr);
  nativebuf = addr_from_java(addr);
  pfree(pool, nativebuf);
  pthread_rwlock_unlock(&g_pmp_rwlock);
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_PMDKPMemServiceImpl_nsync(
    JNIEnv* env,
    jobject this, jlong id, jlong addr, jlong len, jboolean autodetect)
{
  PMPool *pool;
  size_t capa;
  void* nativebuf;
  void* p = addr_from_java(addr);
  if (autodetect) {
    if (NULL != p) {
      nativebuf = p - PMBHSZ;
      pmem_msync(nativebuf, ((PMBHeader *) nativebuf)->size);
    } else {
      pthread_rwlock_rdlock(&g_pmp_rwlock);
      pool = g_pmpool_arr + id;
      capa = pool->capacity;
      pmem_msync(pool->base, capa);
      pthread_rwlock_unlock(&g_pmp_rwlock);
    }
  } else {
    if (NULL != p && len > 0L) {
      pmem_msync(p, len);
    }
  }
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_PMDKPMemServiceImpl_npersist(
    JNIEnv* env,
    jobject this, jlong id, jlong addr, jlong len, jboolean autodetect)
{
  PMPool *pool;
  size_t capa;
  void* nativebuf;
  void* p = addr_from_java(addr);
  if (autodetect) {
    if (NULL != p) {
      nativebuf = p - PMBHSZ;
      pmem_persist(nativebuf, ((PMBHeader *) nativebuf)->size);
    } else {
      pthread_rwlock_rdlock(&g_pmp_rwlock);
      pool = g_pmpool_arr + id;
      capa = pool->capacity;
      pmem_persist(pool->base, capa);
      pthread_rwlock_unlock(&g_pmp_rwlock);
    }
  } else {
    if (NULL != p && len > 0L) {
      pmem_persist(p, len);
    }
  }
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_PMDKPMemServiceImpl_nflush(
    JNIEnv* env,
    jobject this, jlong id, jlong addr, jlong len, jboolean autodetect)
{
  PMPool *pool;
  size_t capa;
  void* nativebuf;
  void* p = addr_from_java(addr);
  if (autodetect) {
    if (NULL != p) {
      nativebuf = p - PMBHSZ;
      pmem_flush(nativebuf, ((PMBHeader *) nativebuf)->size);
    } else {
      pthread_rwlock_rdlock(&g_pmp_rwlock);
      pool = g_pmpool_arr + id;
      capa = pool->capacity;
      pmem_flush(pool->base, capa);
      pthread_rwlock_unlock(&g_pmp_rwlock);
    }
  } else {
    if (NULL != p && len > 0L) {
      pmem_flush(p, len);
    }
  }
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_PMDKPMemServiceImpl_ndrain(
    JNIEnv* env,
    jobject this, jlong id)
{
  pmem_drain();
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_PMDKPMemServiceImpl_ncapacity(
    JNIEnv* env,
    jobject this, jlong id)
{
  PMPool *pool;
  jlong ret;
  pthread_rwlock_rdlock(&g_pmp_rwlock);
  pool = g_pmpool_arr + id;
  ret = pool->capacity;
  pthread_rwlock_unlock(&g_pmp_rwlock);
  return ret;
}

JNIEXPORT
jobject JNICALL Java_org_apache_mnemonic_service_memory_internal_PMDKPMemServiceImpl_ncreateByteBuffer(
    JNIEnv *env, jobject this, jlong id, jlong size) {
  PMPool *pool;
  jobject ret = NULL;
  void* nativebuf = NULL;
  pthread_rwlock_rdlock(&g_pmp_rwlock);
  pool = g_pmpool_arr + id;
  nativebuf = prealloc(pool, NULL, size, 0);
  if (NULL != nativebuf) {
    ret = (*env)->NewDirectByteBuffer(env, nativebuf, size);
  }
  pthread_rwlock_unlock(&g_pmp_rwlock);
  return ret;
}

JNIEXPORT
jobject JNICALL Java_org_apache_mnemonic_service_memory_internal_PMDKPMemServiceImpl_nretrieveByteBuffer(
    JNIEnv *env, jobject this, jlong id, jlong addr) {
  jobject ret = NULL;
  void* p = addr_from_java(addr);
  if (NULL != p) {
    void* nativebuf = p - PMBHSZ;
    ret = (*env)->NewDirectByteBuffer(env, p, ((PMBHeader *) nativebuf)->size - PMBHSZ);
  }
  return ret;
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_PMDKPMemServiceImpl_nretrieveSize(JNIEnv *env,
    jobject this, jlong id, jlong addr) {
  jlong ret = 0L;
  void* p = addr_from_java(addr);
  if (NULL != p) {
    void* nativebuf = p - PMBHSZ;
    ret = ((PMBHeader *) nativebuf)->size - PMBHSZ;
//        fprintf(stderr, "### nretrieveSize size: %lld, %X ###, header size: %ld \n",
//        		((PMBHeader *)nativebuf)->size, nativebuf-b_addr(*(g_pmpool_arr + id)), PMBHSZ);
  }
  return ret;
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_PMDKPMemServiceImpl_ngetByteBufferHandler(
    JNIEnv *env, jobject this, jlong id, jobject bytebuf) {
//	fprintf(stderr, "ngetByteBufferAddress Get Called %X, %X\n", env, bytebuf);
  jlong ret = 0L;
  if (NULL != bytebuf) {
    void* nativebuf = (*env)->GetDirectBufferAddress(env, bytebuf);
//    	fprintf(stderr, "ngetByteBufferAddress Get Native addr %X\n", nativebuf);
    ret = addr_to_java(nativebuf);
  }
//    fprintf(stderr, "ngetByteBufferAddress returned addr %016lx\n", ret);
  return ret;
}

JNIEXPORT
jobject JNICALL Java_org_apache_mnemonic_service_memory_internal_PMDKPMemServiceImpl_nresizeByteBuffer(
    JNIEnv *env, jobject this, jlong id, jobject bytebuf, jlong size) {
  PMPool *pool;
  jobject ret = NULL;
  pthread_rwlock_rdlock(&g_pmp_rwlock);
  pool = g_pmpool_arr + id;
  if (NULL != bytebuf) {
    void* nativebuf = (*env)->GetDirectBufferAddress(env, bytebuf);
    if (nativebuf != NULL) {
      nativebuf = prealloc(pool, nativebuf, size, 0);
      if (NULL != nativebuf) {
        ret = (*env)->NewDirectByteBuffer(env, nativebuf, size);
      }
    }
  }
  pthread_rwlock_unlock(&g_pmp_rwlock);
  return ret;
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_PMDKPMemServiceImpl_ndestroyByteBuffer(
    JNIEnv *env, jobject this, jlong id, jobject bytebuf)
{
  PMPool *pool;
  pthread_rwlock_rdlock(&g_pmp_rwlock);
  pool = g_pmpool_arr + id;
  if (NULL != bytebuf) {
    void* nativebuf = (*env)->GetDirectBufferAddress(env, bytebuf);
    pfree(pool, nativebuf);
  }
  pthread_rwlock_unlock(&g_pmp_rwlock);
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_PMDKPMemServiceImpl_nsetHandler(
    JNIEnv *env, jobject this, jlong id, jlong key, jlong value)
{
  PMPool *pool;
  TOID(struct pmem_root) root;
  pthread_rwlock_rdlock(&g_pmp_rwlock);
  pool = g_pmpool_arr + id;
  root = POBJ_ROOT(pool->pop, struct pmem_root);
  if (key < MAX_HANDLER_STORE_LEN && key >= 0) {
    D_RW(root)->hdl_buf[key] = value;
  }
  pthread_rwlock_unlock(&g_pmp_rwlock);
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_PMDKPMemServiceImpl_ngetHandler(JNIEnv *env,
    jobject this, jlong id, jlong key) {
  PMPool *pool;
  TOID(struct pmem_root) root;
  pthread_rwlock_rdlock(&g_pmp_rwlock);
  pool = g_pmpool_arr + id;
  root = POBJ_ROOT(pool->pop, struct pmem_root);
  jlong ret = (key < MAX_HANDLER_STORE_LEN && key >= 0) ? D_RO(root)->hdl_buf[key] : 0L;
  pthread_rwlock_unlock(&g_pmp_rwlock);
  return ret;
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_PMDKPMemServiceImpl_nhandlerCapacity(
    JNIEnv *env, jobject this) {
  return MAX_HANDLER_STORE_LEN;
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_PMDKPMemServiceImpl_ngetBaseAddress(JNIEnv *env,
    jobject this, jlong id) {
  PMPool *pool;
  jlong ret;
  pthread_rwlock_rdlock(&g_pmp_rwlock);
  pool = g_pmpool_arr + id;
  ret = addr_to_java(pool->base);
  pthread_rwlock_unlock(&g_pmp_rwlock);
  return ret;
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_PMDKPMemServiceImpl_ninit(JNIEnv *env,
    jclass this, jlong capacity, jstring pathname, jboolean isnew) {
  PMPool *pool;
  TOID(struct pmem_root) root;
  int needcreate;
  jlong ret = -1;
  PMEMobjpool *pop = NULL;
  pthread_rwlock_wrlock(&g_pmp_rwlock);
  const char* mpathname = (*env)->GetStringUTFChars(env, pathname, NULL);
  if (NULL == mpathname) {
    pthread_rwlock_unlock(&g_pmp_rwlock);
    throw(env, "Big memory path not specified!");
  }
  needcreate = access(mpathname, F_OK);
  if (isnew && !needcreate) {
    if(0 != unlink(mpathname)) {
      throw(env, "Failure to delete file to create new one.");
    }
    needcreate = 1;
  }
  if (needcreate && capacity < PMEMOBJ_MIN_POOL) {
    capacity = PMEMOBJ_MIN_POOL;
  }
  if (needcreate) {
    pop = pmemobj_create(mpathname, POBJ_LAYOUT_NAME(memory_service), capacity, S_IRUSR | S_IWUSR);
  } else {
    pop = pmemobj_open(mpathname, POBJ_LAYOUT_NAME(memory_service));
  }
  if (pop == NULL) {
    pthread_rwlock_unlock(&g_pmp_rwlock);
    throw(env, "Big memory init failure!");
  }
  (*env)->ReleaseStringUTFChars(env, pathname, mpathname);
  g_pmpool_arr = realloc(g_pmpool_arr, (g_pmpool_count + 1) * sizeof(PMPool));
  if (NULL != g_pmpool_arr) {
    pool = g_pmpool_arr + g_pmpool_count;
    pool->pop = pop;
    root = POBJ_ROOT(pool->pop, struct pmem_root);
    pool->uuid_lo = root.oid.pool_uuid_lo;
    pool->base = (void*)pop;
    if (needcreate) {
      pool->capacity = capacity;
      D_RW(root)->capacity = capacity;
    } else {
      pool->capacity = D_RO(root)->capacity;
    }
    ret = g_pmpool_count;
    ++g_pmpool_count;
  } else {
    pthread_rwlock_unlock(&g_pmp_rwlock);
    throw(env, "Big memory init Out of memory!");
  }
  pthread_rwlock_unlock(&g_pmp_rwlock);
  return ret;
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_PMDKPMemServiceImpl_nclose
(JNIEnv *env, jobject this, jlong id)
{
  PMPool *pool;
  pthread_rwlock_wrlock(&g_pmp_rwlock);
  pool = g_pmpool_arr + id;
  if (NULL != pool->pop) {
    pmemobj_close(pool->pop);
    pool->pop = NULL;
    pool->base = NULL;
    pool->uuid_lo = 0;
    pool->capacity = 0;
  }
  pthread_rwlock_unlock(&g_pmp_rwlock);
}

__attribute__((destructor)) void fini(void) {
  int i;
  PMPool *pool;
  pthread_rwlock_wrlock(&g_pmp_rwlock);
  if (NULL != g_pmpool_arr) {
    for (i = 0; i < g_pmpool_count; ++i) {
      pool = g_pmpool_arr + i;
      if (NULL != pool->pop) {
        pmemobj_close(pool->pop);
        pool->pop = NULL;
        pool->base = NULL;
        pool->capacity = 0;
        pool->uuid_lo = 0;
      }
    }
    free(g_pmpool_arr);
    g_pmpool_arr = NULL;
    g_pmpool_count = 0;
  }
  pthread_rwlock_unlock(&g_pmp_rwlock);
  pthread_rwlock_destroy(&g_pmp_rwlock);
}
