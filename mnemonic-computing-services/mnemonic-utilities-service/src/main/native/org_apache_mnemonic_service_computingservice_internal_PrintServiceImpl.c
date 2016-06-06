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

#include "org_apache_mnemonic_service_computingservice_internal_PrintServiceImpl.h"

/******************************************************************************
 ** JNI implementations
 *****************************************************************************/

JNIEXPORT
jlong JNICALL Java_org_apache_mnemonic_service_computingservice_internal_PrintServiceImpl_nperformPrint(JNIEnv* env,
    jobject this, jlong hdr, jobjectArray arr) {
  printf("----Service Native Parameters----\n");
  printf("Handler --> %p \n", addr_from_java(hdr));
  jsize i, j; jlong *vals;
  jlongArray *larr;
  jsize len1 = (*env)->GetArrayLength(env, arr);
  jsize len2;
  for (i = 0; i < len1; ++i) {
    larr = (jlongArray)((*env)->GetObjectArrayElement(env, arr, i));
    len2 = (*env)->GetArrayLength(env, larr);
    vals = (*env)->GetLongArrayElements(env, larr, 0);
    printf("Stack Row %d - ", i);
    for (j = 0; j < len2; ++j) {
      printf("[%d]:%p ", j, vals[j]);
    }
    printf("\n");
    (*env)->ReleaseLongArrayElements(env, larr, vals, 0);
  }

  return 0L;
}

__attribute__((destructor)) void fini(void) {
}
