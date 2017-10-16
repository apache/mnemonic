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

#include "org_apache_mnemonic_service_computing_internal_VectorizationServiceImpl.h"

/******************************************************************************
 ** JNI implementations
 *****************************************************************************/

/**
 * a customized handler as value handler.
 * It handles would be used to iteratively callback for each value of a value matrix
 */
void valVectorizationHandler(JNIEnv* env, size_t dims[], size_t dimidx,
    long *itmaddrs[], long *(* const nxtfitmaddrs)[], long (* const pendings)[],
    void *addr, size_t sz, int dtype, void *dc_addr, long dc_size, long *position, long *count) {

  uint64_t buffer_size = (uint64_t)(dc_addr - 8); // DurableChunk Size is 8 bytes before starting of the chunk

  if (buffer_size < dc_size) {
    printf("Not enough space for element vectorization");
    return;
  }

  if (*position >= dc_size) {
    printf("Memory Chunk is full!");
    return;
  }

  switch(dtype) {
    case DURABLE_BOOLEAN:
    case DURABLE_CHARACTER:
    case DURABLE_BYTE:
      if (*position + 1 < dc_size) {
        *position += 1;
        *count += 1;
      }
      break;
    case DURABLE_SHORT:
      if (*position + 2 < dc_size) {
        *position += 2;
        *count += 1;
      }
      break;
    case DURABLE_INTEGER:
      if (*position + 4 < dc_size) {
        *position += 4;
        *count += 1;
      }
      break;
    case DURABLE_LONG:
      if (*position + 8 < dc_size) {
        *position += 8;
        *count += 1;
      }
      break;
    case DURABLE_FLOAT:
      if (*position + 4 < dc_size) {
        *position += 4;
        *count += 1;
      }
      break;
    case DURABLE_DOUBLE:
      if (*position + 8 < dc_size) {
        *position += 8;
        *count += 1;
      }
      break;
    default:
      return;
  }

}

/**
 * It is invoked by Java side computing service.
 * a step of computation should be handle in a whole by this function.
 * param. vinfos contains all relevant value matrixes for this computation.
 * It could return a set of handlers as results or in-place update the data indicated by value matrixes.
 */
JNIEXPORT
jlongArray JNICALL Java_org_apache_mnemonic_service_computingservice_internal_VectorizationServiceImpl_nperformVectorization(JNIEnv* env,
    jobject this, jobjectArray vinfos, jlong chk_hdl, jlong chk_sz) {
  jlongArray ret = NULL;
  jsize retsz = 1;
  jsize idx;
  jlong nret[retsz];
  jlong cnt = 0;

  for(idx = 0; idx < retsz; ++idx) {
    nret[idx] = 0L;
  }
  size_t visz;
  struct NValueInfo **nvinfos = constructNValueInfos(env, vinfos, &visz);
  printNValueInfos(nvinfos, visz);
  for(idx = 0; idx < visz; ++idx) {
    printf("-- Value Matrix #%u --\n", idx);
    if (NULL != nvinfos + idx) {
      handleVectorInfo(env, *(nvinfos + idx), valVectorizationHandler, chk_hdl, chk_sz, &cnt);
    } else {
      printf("NULL\n");
    }
  }

  nret[0] = cnt;

  ret = constructJLongArray(env, nret, retsz);
  destructNValueInfos(nvinfos, visz);
  return ret;
}

/*
__attribute__((destructor)) void fini(void) {
}
*/
