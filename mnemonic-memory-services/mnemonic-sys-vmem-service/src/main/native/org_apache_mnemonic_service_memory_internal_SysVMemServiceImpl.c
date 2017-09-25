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

#include "org_apache_mnemonic_service_memory_internal_SysVMemServiceImpl.h"

static SysVMPool *g_sysvmpool_arr = NULL;
static size_t g_sysvmpool_count = 0;

static pthread_rwlock_t g_sysvmem_rwlock = PTHREAD_RWLOCK_INITIALIZER;

/******************************************************************************
 ** JNI implementations
 *****************************************************************************/

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_SysVMemServiceImpl_nallocate(JNIEnv* env,
    jobject this, jlong id, jlong size, jboolean initzero) {
  SysVMPool *pool;
  pthread_rwlock_rdlock(&g_sysvmem_rwlock);
  pool = g_sysvmpool_arr + id;
  void* nativebuf = sysvrealloc(pool, NULL, size, initzero);
  pthread_rwlock_unlock(&g_sysvmem_rwlock);
  return addr_to_java(nativebuf);
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_SysVMemServiceImpl_nreallocate(JNIEnv* env,
    jobject this, jlong id, jlong addr, jlong size, jboolean initzero) {
  SysVMPool *pool;
  pthread_rwlock_rdlock(&g_sysvmem_rwlock);
  pool = g_sysvmpool_arr + id;
  void* p = addr_from_java(addr);
  void* nativebuf = sysvrealloc(pool, p, size, initzero);
  pthread_rwlock_unlock(&g_sysvmem_rwlock);
  return addr_to_java(nativebuf);
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_SysVMemServiceImpl_nfree(
    JNIEnv* env,
    jobject this, jlong id,
    jlong addr) {
  SysVMPool *pool;
  pthread_rwlock_rdlock(&g_sysvmem_rwlock);
  pool = g_sysvmpool_arr + id;
  void* nativebuf = addr_from_java(addr);
  sysvfree(pool, nativebuf);
  pthread_rwlock_unlock(&g_sysvmem_rwlock);
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_SysVMemServiceImpl_nsync(
    JNIEnv* env,
    jobject this, jlong id, jlong addr, jlong len, jboolean autodetect)
{
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_SysVMemServiceImpl_ncapacity(
    JNIEnv* env,
    jobject this, jlong id)
{
//  SysVMPool *pool;
//  pool = g_sysvmpool_arr + id;
  return 0L;
}

JNIEXPORT
jobject JNICALL Java_org_apache_mnemonic_service_memory_internal_SysVMemServiceImpl_ncreateByteBuffer(
    JNIEnv *env, jobject this, jlong id, jlong size) {
  SysVMPool *pool;
  jobject ret = NULL;
  pthread_rwlock_rdlock(&g_sysvmem_rwlock);
  pool = g_sysvmpool_arr + id;
  void* nativebuf = sysvrealloc(pool, NULL, size, 0);
  ret = NULL != nativebuf ? (*env)->NewDirectByteBuffer(env, nativebuf, size) : NULL;
  pthread_rwlock_unlock(&g_sysvmem_rwlock);
  return ret;
}

JNIEXPORT
jobject JNICALL Java_org_apache_mnemonic_service_memory_internal_SysVMemServiceImpl_nretrieveByteBuffer(
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
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_SysVMemServiceImpl_nretrieveSize(JNIEnv *env,
    jobject this, jlong id, jlong addr) {
  SysVMPool *pool;
  pthread_rwlock_rdlock(&g_sysvmem_rwlock);
  pool = g_sysvmpool_arr + id;
  void* p = addr_from_java(addr);
  jlong ret = sysvsize(pool, p);
  pthread_rwlock_unlock(&g_sysvmem_rwlock);
  return ret;
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_SysVMemServiceImpl_ngetByteBufferHandler(
    JNIEnv *env, jobject this, jlong id, jobject bytebuf) {
  jlong ret = 0L;
  if (NULL != bytebuf) {
    void* nativebuf = (*env)->GetDirectBufferAddress(env, bytebuf);
    ret = addr_to_java(nativebuf);
  }
  return ret;
}

JNIEXPORT
jobject JNICALL Java_org_apache_mnemonic_service_memory_internal_SysVMemServiceImpl_nresizeByteBuffer(
    JNIEnv *env, jobject this, jlong id, jobject bytebuf, jlong size) {
  SysVMPool *pool;
  jobject ret = NULL;
  pthread_rwlock_rdlock(&g_sysvmem_rwlock);
  pool = g_sysvmpool_arr + id;
  if (NULL != bytebuf) {
    void* nativebuf = (void*) (*env)->GetDirectBufferAddress(env, bytebuf);
    if (nativebuf != NULL) {
      nativebuf = sysvrealloc(pool, nativebuf, size, 0);
      if (NULL != nativebuf) {
        ret = (*env)->NewDirectByteBuffer(env, nativebuf, size);
      }
    }
  }
  pthread_rwlock_unlock(&g_sysvmem_rwlock);
  return ret;
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_SysVMemServiceImpl_ndestroyByteBuffer(
    JNIEnv *env, jobject this, jlong id, jobject bytebuf) {
  SysVMPool *pool;
  pthread_rwlock_rdlock(&g_sysvmem_rwlock);
  pool = g_sysvmpool_arr + id;
  if (NULL != bytebuf) {
    void* nativebuf = (*env)->GetDirectBufferAddress(env, bytebuf);
    if (nativebuf != NULL) {
      sysvfree(pool, nativebuf);
    }
  }
  pthread_rwlock_unlock(&g_sysvmem_rwlock);
}


JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_SysVMemServiceImpl_nsetHandler(
    JNIEnv *env, jobject this, jlong id, jlong key, jlong value)
{
  throw(env, "setkey()/getkey() temporarily not supported");
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_SysVMemServiceImpl_ngetHandler(JNIEnv *env,
    jobject this, jlong id, jlong key) {
  throw(env, "setkey()/getkey() temporarily not supported");
  return 0;
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_SysVMemServiceImpl_nhandlerCapacity(
    JNIEnv *env, jobject this) {
  throw(env, "setkey()/getkey() temporarily not supported");
  return 0;
}


JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_SysVMemServiceImpl_ngetBaseAddress(JNIEnv *env,
    jobject this, jlong id) {
  return 0L;
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_SysVMemServiceImpl_ninit(JNIEnv *env, jclass this,
    jlong capacity, jstring pathname, jboolean isnew) {
  pthread_rwlock_wrlock(&g_sysvmem_rwlock);
//  SysVMPool *pool;
  size_t ret = -1;
  g_sysvmpool_arr = realloc(g_sysvmpool_arr, (g_sysvmpool_count + 1) * sizeof(SysVMPool));
  if (NULL != g_sysvmpool_arr) {
//    pool = g_sysvmpool_arr + g_sysvmpool_count;
    ret = g_sysvmpool_count;
    g_sysvmpool_count++;
  } else {
    pthread_rwlock_unlock(&g_sysvmem_rwlock);
    throw(env, "Out of memory!");
  }
  pthread_rwlock_unlock(&g_sysvmem_rwlock);
  return ret;
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_SysVMemServiceImpl_nclose
(JNIEnv *env, jobject this, jlong id)
{
//  SysVMPool *pool;
//  pthread_rwlock_wrlock(&g_sysvmem_rwlock);
//  pool = g_sysvmpool_arr + id;
//  pthread_rwlock_unlock(&g_sysvmem_rwlock);
}

__attribute__((destructor)) void fini(void) {
//  int i;
//  SysVMPool *pool;
  pthread_rwlock_wrlock(&g_sysvmem_rwlock);
  if (NULL != g_sysvmpool_arr) {
//    for (i = 0; i < g_sysvmpool_count; ++i) {
//      pool = g_sysvmpool_arr + i;
//    }
    free(g_sysvmpool_arr);
    g_sysvmpool_arr = NULL;
    g_sysvmpool_count = 0;
  }
  pthread_rwlock_unlock(&g_sysvmem_rwlock);
  pthread_rwlock_destroy(&g_sysvmem_rwlock);
}
