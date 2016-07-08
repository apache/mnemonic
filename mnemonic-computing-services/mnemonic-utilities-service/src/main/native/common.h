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

#ifndef _COMMON_H
#define _COMMON_H
#ifdef __cplusplus
extern "C" {
#endif

#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <sys/stat.h>
#include <stdint.h>
#include <assert.h>
#include <pthread.h>
#include <jni.h>

#define DURABLE_BOOLEAN 1
#define DURABLE_CHARACTER 2
#define DURABLE_BYTE 3
#define DURABLE_SHORT 4
#define DURABLE_INTEGER 5
#define DURABLE_LONG 6
#define DURABLE_FLOAT 7
#define DURABLE_DOUBLE 8
#define DURABLE_STRING 9
#define DURABLE_DURABLE 10
#define DURABLE_BUFFER 11
#define DURABLE_CHUNK 12

void throw(JNIEnv* env, const char* msg);

void* addr_from_java(jlong addr);

jlong addr_to_java(void* p);

struct transitem {
  long hdlbase;
  long size;
  void* base;
};

struct frameitem {
  long nextoff;
  long nextsz;
  long nlvloff;
  long nlvlsz;
};

struct NValueInfo {
  long handler;
  struct transitem *transtable;
  size_t transtablesz;
  struct frameitem *frames;
  size_t framessz;
  int dtype;
};

typedef void (*valueHandler)(JNIEnv* env, size_t dims[], size_t dimsz,
    void *addr, size_t sz, int dtype);

struct NValueInfo **constructNValueInfos(JNIEnv* env,
    jobjectArray vinfos, size_t *sz);

void destructNValueInfos(struct NValueInfo **nvalinfos, size_t sz);

void printNValueInfos(struct NValueInfo **nvalinfos, size_t sz);

jlongArray constructJLongArray(JNIEnv* env, long arr[], size_t sz);

inline void *to_e(JNIEnv* env, struct NValueInfo *nvinfo, long p);

inline long to_p(JNIEnv* env, struct NValueInfo *nvinfo, void *e);

int handleValueInfo(JNIEnv* env, struct NValueInfo *nvinfo, valueHandler valhandler);

#ifdef __cplusplus
}
#endif
#endif
