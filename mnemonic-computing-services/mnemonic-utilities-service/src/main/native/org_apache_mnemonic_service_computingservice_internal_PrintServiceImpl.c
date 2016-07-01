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
jlongArray JNICALL Java_org_apache_mnemonic_service_computingservice_internal_PrintServiceImpl_nperformPrint(JNIEnv* env,
    jobject this, jobjectArray vinfos) {
  jlongArray ret = NULL;
  jsize retsz = 1;
  jsize idx;
  jlong nret[retsz];
  for(idx = 0; idx < retsz; ++idx) {
    nret[idx] = 0L;
  }
  size_t visz;
  struct NValueInfo **nvinfos = constructNValueInfos(env, vinfos, &visz);


  printf("----Service Native Parameters----\n");
  printNValueInfos(nvinfos, visz);

  ret = constructJLongArray(env, nret, retsz);
  destructNValueInfos(nvinfos, visz);
  return ret;
}

__attribute__((destructor)) void fini(void) {
}
