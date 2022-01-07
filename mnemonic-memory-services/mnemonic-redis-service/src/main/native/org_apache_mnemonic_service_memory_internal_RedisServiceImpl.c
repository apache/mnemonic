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

#include "org_apache_mnemonic_service_memory_internal_RedisServiceImpl.h"

static pthread_rwlock_t g_pmalp_rwlock = PTHREAD_RWLOCK_INITIALIZER;

/******************************************************************************
 ** JNI implementations
 *****************************************************************************/

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_RedisServiceImpl_nallocate(JNIEnv* env,
    jobject this, jlong id, jlong size, jboolean initzero) {
  jlong ret = 0L;
  pthread_rwlock_rdlock(&g_pmalp_rwlock);
  pthread_rwlock_unlock(&g_pmalp_rwlock);
  return ret;
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_RedisServiceImpl_nreallocate(JNIEnv* env,
    jobject this, jlong id, jlong addr, jlong size, jboolean initzero) {
  jlong ret = 0L;
  pthread_rwlock_rdlock(&g_pmalp_rwlock);
  pthread_rwlock_unlock(&g_pmalp_rwlock);
  return ret;
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_RedisServiceImpl_nfree(
    JNIEnv* env,
    jobject this, jlong id,
    jlong addr) {
  pthread_rwlock_rdlock(&g_pmalp_rwlock);
  pthread_rwlock_unlock(&g_pmalp_rwlock);
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_RedisServiceImpl_nsync(
    JNIEnv* env,
    jobject this, jlong id, jlong addr, jlong len, jboolean autodetect) {
  pthread_rwlock_rdlock(&g_pmalp_rwlock);
  pthread_rwlock_unlock(&g_pmalp_rwlock);
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_RedisServiceImpl_npersist(
    JNIEnv* env,
    jobject this, jlong id, jlong addr, jlong len, jboolean autodetect) {
  pthread_rwlock_rdlock(&g_pmalp_rwlock);
  pthread_rwlock_unlock(&g_pmalp_rwlock);
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_RedisServiceImpl_nflush(
    JNIEnv* env,
    jobject this, jlong id, jlong addr, jlong len, jboolean autodetect) {
  pthread_rwlock_rdlock(&g_pmalp_rwlock);
  pthread_rwlock_unlock(&g_pmalp_rwlock);
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_RedisServiceImpl_ndrain(
    JNIEnv* env,
    jobject this, jlong id)
{
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_RedisServiceImpl_ncapacity(
    JNIEnv* env,
    jobject this, jlong id) {
  jlong ret = 0L;
  pthread_rwlock_rdlock(&g_pmalp_rwlock);
  pthread_rwlock_unlock(&g_pmalp_rwlock);
  return ret;
}

JNIEXPORT
jobject JNICALL Java_org_apache_mnemonic_service_memory_internal_RedisServiceImpl_ncreateByteBuffer(
    JNIEnv *env, jobject this, jlong id, jlong size) {
  jobject ret = NULL;
  pthread_rwlock_rdlock(&g_pmalp_rwlock);
  pthread_rwlock_unlock(&g_pmalp_rwlock);
  return ret;
}

JNIEXPORT
jobject JNICALL Java_org_apache_mnemonic_service_memory_internal_RedisServiceImpl_nretrieveByteBuffer(
    JNIEnv *env, jobject this, jlong id, jlong addr) {
  jobject ret = NULL;
  return ret;
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_RedisServiceImpl_nretrieveSize(JNIEnv *env,
    jobject this, jlong id, jlong addr) {
  jlong ret = 0L;
  pthread_rwlock_rdlock(&g_pmalp_rwlock);
  pthread_rwlock_unlock(&g_pmalp_rwlock);
  return ret;
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_RedisServiceImpl_ngetByteBufferHandler(
    JNIEnv *env, jobject this, jlong id, jobject bytebuf) {
  jlong ret = 0L;
  return ret;
}

JNIEXPORT
jobject JNICALL Java_org_apache_mnemonic_service_memory_internal_RedisServiceImpl_nresizeByteBuffer(
    JNIEnv *env, jobject this, jlong id, jobject bytebuf, jlong size) {
  pthread_rwlock_rdlock(&g_pmalp_rwlock);
  jobject ret = NULL;
  pthread_rwlock_unlock(&g_pmalp_rwlock);
  return ret;
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_RedisServiceImpl_ndestroyByteBuffer(
    JNIEnv *env, jobject this, jlong id, jobject bytebuf) {
  pthread_rwlock_rdlock(&g_pmalp_rwlock);
  pthread_rwlock_unlock(&g_pmalp_rwlock);
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_RedisServiceImpl_nsetHandler(
    JNIEnv *env, jobject this, jlong id, jlong key, jlong value) {
  pthread_rwlock_rdlock(&g_pmalp_rwlock);
  pthread_rwlock_unlock(&g_pmalp_rwlock);
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_RedisServiceImpl_ngetHandler(JNIEnv *env,
    jobject this, jlong id, jlong key) {
  pthread_rwlock_rdlock(&g_pmalp_rwlock);
  jlong ret = 0L;
  pthread_rwlock_unlock(&g_pmalp_rwlock);
  return ret;
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_RedisServiceImpl_nhandlerCapacity(
    JNIEnv *env, jobject this) {
  return 0L;
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_RedisServiceImpl_ngetBaseAddress(JNIEnv *env,
    jobject this, jlong id) {
  pthread_rwlock_rdlock(&g_pmalp_rwlock);
  jlong ret = 0L;
  pthread_rwlock_unlock(&g_pmalp_rwlock);
  return ret;
}

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_memory_internal_RedisServiceImpl_ninit(JNIEnv *env,
    jclass this, jlong capacity, jstring pathname, jboolean isnew) {
  size_t ret = -1;
  pthread_rwlock_wrlock(&g_pmalp_rwlock);
  pthread_rwlock_unlock(&g_pmalp_rwlock);
  return ret;
}

JNIEXPORT
void JNICALL Java_org_apache_mnemonic_service_memory_internal_RedisServiceImpl_nclose
(JNIEnv *env, jobject this, jlong id)
{
  pthread_rwlock_wrlock(&g_pmalp_rwlock);
  pthread_rwlock_unlock(&g_pmalp_rwlock);
}

__attribute__((destructor)) void fini(void) {
  pthread_rwlock_unlock(&g_pmalp_rwlock);
  pthread_rwlock_destroy(&g_pmalp_rwlock);
}
