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

#include "org_apache_mnemonic_service_memory_internal_VMemServiceImpl.h"

static VMPool *g_vmpool_arr = NULL;
static size_t g_vmpool_count = 0;

static pthread_rwlock_t g_vmem_rwlock = PTHREAD_RWLOCK_INITIALIZER;

/******************************************************************************
 ** JNI implementations
 *****************************************************************************/

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_VMemServiceImpl_nallocate(JNIEnv* env,
    jobject this, jlong id, jlong size, jboolean initzero) {
  VMPool *pool;
  pthread_rwlock_rdlock(&g_vmem_rwlock);
  pool = g_vmpool_arr + id;
  pthread_mutex_lock(&pool->mutex);
  void* nativebuf = vrealloc(pool, NULL, size, initzero);
  pthread_mutex_unlock(&pool->mutex);
  pthread_rwlock_unlock(&g_vmem_rwlock);
  return addr_to_java(nativebuf);
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_VMemServiceImpl_nreallocate(JNIEnv* env,
    jobject this, jlong id, jlong addr, jlong size, jboolean initzero) {
  VMPool *pool;
  pthread_rwlock_rdlock(&g_vmem_rwlock);
  pool = g_vmpool_arr + id;
  pthread_mutex_lock(&pool->mutex);
  void* p = addr_from_java(addr);
  void* nativebuf = vrealloc(pool, p, size, initzero);
  pthread_mutex_unlock(&pool->mutex);
  pthread_rwlock_unlock(&g_vmem_rwlock);
  return addr_to_java(nativebuf);
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_VMemServiceImpl_nfree(
    JNIEnv* env,
    jobject this, jlong id,
    jlong addr) {
  VMPool *pool;
  pthread_rwlock_rdlock(&g_vmem_rwlock);
  pool = g_vmpool_arr + id;
  pthread_mutex_lock(&pool->mutex);
  void* nativebuf = addr_from_java(addr);
  vfree(pool, nativebuf);
  pthread_mutex_unlock(&pool->mutex);
  pthread_rwlock_unlock(&g_vmem_rwlock);
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_VMemServiceImpl_nsync(
    JNIEnv* env,
    jobject this, jlong id, jlong addr, jlong len, jboolean autodetect)
{
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_VMemServiceImpl_ncapacity(
    JNIEnv* env,
    jobject this, jlong id)
{
  VMPool *pool;
  pool = g_vmpool_arr + id;
  return pool->capacity;
}

JNIEXPORT
jobject JNICALL Java_org_apache_mnemonic_service_memory_internal_VMemServiceImpl_ncreateByteBuffer(
    JNIEnv *env, jobject this, jlong id, jlong size) {
  VMPool *pool;
  jobject ret = NULL;
  pthread_rwlock_rdlock(&g_vmem_rwlock);
  pool = g_vmpool_arr + id;
  pthread_mutex_lock(&pool->mutex);
  void* nativebuf = vrealloc(pool, NULL, size, 0);
  ret = NULL != nativebuf ? (*env)->NewDirectByteBuffer(env, nativebuf, size) : NULL;
  pthread_mutex_unlock(&pool->mutex);
  pthread_rwlock_unlock(&g_vmem_rwlock);
  return ret;
}

JNIEXPORT
jobject JNICALL Java_org_apache_mnemonic_service_memory_internal_VMemServiceImpl_nretrieveByteBuffer(
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
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_VMemServiceImpl_nretrieveSize(JNIEnv *env,
    jobject this, jlong id, jlong addr) {
  VMPool *pool;
  pthread_rwlock_rdlock(&g_vmem_rwlock);
  pool = g_vmpool_arr + id;
  void* p = addr_from_java(addr);
  jlong ret = vsize(pool, p);
  pthread_rwlock_unlock(&g_vmem_rwlock);
  return ret;
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_VMemServiceImpl_ngetByteBufferHandler(
    JNIEnv *env, jobject this, jlong id, jobject bytebuf) {
//  fprintf(stderr, "ngetByteBufferAddress Get Called %X, %X\n", env, bytebuf);
  jlong ret = 0L;
  if (NULL != bytebuf) {
    void* nativebuf = (*env)->GetDirectBufferAddress(env, bytebuf);
//      fprintf(stderr, "ngetByteBufferAddress Get Native addr %X\n", nativebuf);
    ret = addr_to_java(nativebuf);
  }
//    fprintf(stderr, "ngetByteBufferAddress returned addr %016lx\n", ret);
  return ret;
}

JNIEXPORT
jobject JNICALL Java_org_apache_mnemonic_service_memory_internal_VMemServiceImpl_nresizeByteBuffer(
    JNIEnv *env, jobject this, jlong id, jobject bytebuf, jlong size) {
  VMPool *pool;
  jobject ret = NULL;
  pthread_rwlock_rdlock(&g_vmem_rwlock);
  pool = g_vmpool_arr + id;
  pthread_mutex_lock(&pool->mutex);
  if (NULL != bytebuf) {
    void* nativebuf = (void*) (*env)->GetDirectBufferAddress(env, bytebuf);
    if (nativebuf != NULL) {
      nativebuf = vrealloc(pool, nativebuf, size, 0);
      if (NULL != nativebuf) {
        ret = (*env)->NewDirectByteBuffer(env, nativebuf, size);
      }
    }
  }
  pthread_mutex_unlock(&pool->mutex);
  pthread_rwlock_unlock(&g_vmem_rwlock);
  return ret;
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_VMemServiceImpl_ndestroyByteBuffer(
    JNIEnv *env, jobject this, jlong id, jobject bytebuf) {
  VMPool *pool;
  pthread_rwlock_rdlock(&g_vmem_rwlock);
  pool = g_vmpool_arr + id;
  pthread_mutex_lock(&pool->mutex);
  if (NULL != bytebuf) {
    void* nativebuf = (*env)->GetDirectBufferAddress(env, bytebuf);
    if (nativebuf != NULL) {
      vfree(pool, nativebuf);
    }
  }
  pthread_mutex_unlock(&pool->mutex);
  pthread_rwlock_unlock(&g_vmem_rwlock);
}


JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_VMemServiceImpl_nsetHandler(
    JNIEnv *env, jobject this, jlong id, jlong key, jlong value)
{
  throw(env, "setkey()/getkey() temporarily not supported");
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_VMemServiceImpl_ngetHandler(JNIEnv *env,
    jobject this, jlong id, jlong key) {
  throw(env, "setkey()/getkey() temporarily not supported");
  return 0;
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_VMemServiceImpl_nhandlerCapacity(
    JNIEnv *env, jobject this) {
  throw(env, "setkey()/getkey() temporarily not supported");
  return 0;
}


JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_VMemServiceImpl_ngetBaseAddress(JNIEnv *env,
    jobject this, jlong id) {
  return 0L;
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_VMemServiceImpl_ninit(JNIEnv *env, jclass this,
    jlong capacity, jstring pathname, jboolean isnew) {
  pthread_rwlock_wrlock(&g_vmem_rwlock);
  VMPool *pool;
  size_t ret = -1;
  VMEM *vmp = NULL;
  const char* mpathname = (*env)->GetStringUTFChars(env, pathname, NULL);
  if (NULL == mpathname) {
    pthread_rwlock_unlock(&g_vmem_rwlock);
    throw(env, "Big memory path not specified!");
  }
  if ((vmp = vmem_create(mpathname,
       VMEM_MIN_POOL > capacity ? VMEM_MIN_POOL : capacity)) == NULL) {
    pthread_rwlock_unlock(&g_vmem_rwlock);
    throw(env, "Big memory init failure!");
  }
  g_vmpool_arr = realloc(g_vmpool_arr, (g_vmpool_count + 1) * sizeof(VMPool));
  if (NULL != g_vmpool_arr) {
    pool = g_vmpool_arr + g_vmpool_count;
    pool->vmp = vmp;
    pool->capacity = capacity;
    pthread_mutex_init(&pool->mutex, NULL);
    ret = g_vmpool_count;
    g_vmpool_count++;
  } else {
    pthread_rwlock_unlock(&g_vmem_rwlock);
    throw(env, "Big memory init Out of memory!");
  }
  pthread_rwlock_unlock(&g_vmem_rwlock);
  return ret;
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_VMemServiceImpl_nclose
(JNIEnv *env, jobject this, jlong id)
{
  VMPool *pool;
  pthread_rwlock_wrlock(&g_vmem_rwlock);
  pool = g_vmpool_arr + id;
  pthread_mutex_lock(&pool->mutex);
  if (NULL != pool->vmp) {
    pool->vmp = NULL;
    pool->capacity = 0;
  }
  pthread_mutex_unlock(&pool->mutex);
  pthread_mutex_destroy(&pool->mutex);
  pthread_rwlock_unlock(&g_vmem_rwlock);
}

__attribute__((destructor)) void fini(void) {
  int i;
  VMPool *pool;
  pthread_rwlock_wrlock(&g_vmem_rwlock);
  if (NULL != g_vmpool_arr) {
    for (i = 0; i < g_vmpool_count; ++i) {
      pool = g_vmpool_arr + i;
      if (NULL != pool->vmp) {
        pool->vmp = NULL;
        pool->capacity = 0;
        pthread_mutex_destroy(&pool->mutex);
      }
    }
    free(g_vmpool_arr);
    g_vmpool_arr = NULL;
    g_vmpool_count = 0;
  }
  pthread_rwlock_unlock(&g_vmem_rwlock);
  pthread_rwlock_destroy(&g_vmem_rwlock);
}
