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

#include "org_apache_mnemonic_service_computing_internal_PrintServiceImpl.h"

/******************************************************************************
 ** JNI implementations
 *****************************************************************************/

/**
 * a customized handler as value handler.
 * It handles would be used to iteratively callback for each value of a value matrix
 */
void valPrintHandler(JNIEnv* env, size_t dims[], size_t dimidx,
    long *itmaddrs[], long *(* const nxtfitmaddrs)[], long (* const pendings)[],
    void *addr, size_t sz, int dtype) {
  size_t i;
  if (0 == dims[dimidx]) {
    printf("\n");
  }
  for (i = 0; i <= dimidx; ++i) {
    printf("[%zu]", dims[i]);
  }
  printf(" <%p, %p> ", itmaddrs[dimidx], (*nxtfitmaddrs)[dimidx]);
  switch(dtype) {
  case DURABLE_BOOLEAN:
    printf(" %s, ", (0 == *(char*)addr ? "T" : "F"));
    break;
  case DURABLE_CHARACTER:
    printf(" %c, ", *(char*)addr);
    break;
  case DURABLE_BYTE:
    printf(" %d, ", *(char*)addr);
    break;
  case DURABLE_SHORT:
    printf(" %d, ", *(short*)addr);
    break;
  case DURABLE_INTEGER:
    printf(" %d, ", *(int*)addr);
    break;
  case DURABLE_LONG:
    printf(" %ld, ", *(long*)addr);
    break;
  case DURABLE_FLOAT:
    printf(" %f, ", *(float*)addr);
    break;
  case DURABLE_DOUBLE:
    printf(" %f, ", *(double*)addr);
    break;
  default:
    printf(" (NA), ");
  }
}

/**
 * It is invoked by Java side computing service.
 * a step of computation should be handle in a whole by this function.
 * param. vinfos contains all relevant value matrixes for this computation.
 * It could return a set of handlers as results or in-place update the data indicated by value matrixes.
 */
JNIEXPORT
jlongArray JNICALL Java_org_apache_mnemonic_service_computing_internal_PrintServiceImpl_nperformPrint(JNIEnv* env,
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
  printNValueInfos(nvinfos, visz);
  for(idx = 0; idx < visz; ++idx) {
    printf("-- Value Matrix #%u --\n", idx);
    if (NULL != nvinfos + idx) {
      handleValueInfo(env, *(nvinfos + idx), valPrintHandler);
    } else {
      printf("NULL\n");
    }
  }

  ret = constructJLongArray(env, nret, retsz);
  destructNValueInfos(nvinfos, visz);
  return ret;
}

/*
__attribute__((destructor)) void fini(void) {
}
*/
