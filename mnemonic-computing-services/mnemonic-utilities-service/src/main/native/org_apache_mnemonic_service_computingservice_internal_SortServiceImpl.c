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

#include "org_apache_mnemonic_service_computingservice_internal_SortServiceImpl.h"

/******************************************************************************
 ** JNI implementations
 *****************************************************************************/

int compareLongValue(long val1, long val2) {
  if (val1 == val2) {
    return 0;
  }
  return val1 < val2 ? -1 : 1;
}

int compareDoubleValue(double val1, double val2) {
  if (fabs(val1 - val2) <= FLT_EPSILON) {
    return 0;
  }
  return val1 < val2 ? -1 : 1;
}

int compareValues(int dtype, void *val1ptr, void *val2ptr) {
  int ret = 0;
  if (NULL != val1ptr && NULL != val2ptr) {
    switch(dtype) {
    case DURABLE_BOOLEAN:
    case DURABLE_CHARACTER:
    case DURABLE_BYTE:
      ret = memcmp(val1ptr, val2ptr, 1);
      break;
    case DURABLE_SHORT:
      ret = compareLongValue(*(short*)val1ptr, *(short*)val2ptr);
      break;
    case DURABLE_INTEGER:
      ret = compareLongValue(*(int*)val1ptr, *(int*)val2ptr);
      break;
    case DURABLE_LONG:
      ret = compareLongValue(*(long*)val1ptr, *(long*)val2ptr);
      break;
    case DURABLE_FLOAT:
      ret = compareDoubleValue(*(float*)val1ptr, *(float*)val2ptr);
      break;
    case DURABLE_DOUBLE:
      ret = compareDoubleValue(*(double*)val1ptr, *(double*)val2ptr);
      break;
    }
  }
  return ret;
}

/**
 * a customized handler as value handler.
 * It handles would be used to iteratively callback for each value of a value matrix
 */
void valSimpleSortHandler(JNIEnv* env, size_t dims[], size_t dimidx,
    long *itmaddrs[], long *(* const nxtfitmaddrs)[], long (* const pendings)[],
    void *addr, size_t sz, int dtype) {
  static long *hptr1 = NULL, *hptr2 = NULL;
  static void *pvaladdr = NULL;
  long *tmpptr = NULL;
  long tmpval = 0L;
  if (dimidx >= 0) {
    if (NULL == pvaladdr) {
      pvaladdr = addr;
      hptr2 = itmaddrs[dimidx];
    } else {
      hptr1 = hptr2;
      hptr2 = itmaddrs[dimidx];
      if (compareValues(dtype, pvaladdr, addr) > 0) {
        tmpptr = (*nxtfitmaddrs)[dimidx];
        tmpval = *tmpptr;
        *tmpptr = *hptr1;
        *hptr1 = *hptr2;
        *hptr2 = tmpval;
        (*nxtfitmaddrs)[dimidx] = hptr2;
        hptr2 = tmpptr;
        ++(*pendings)[dimidx];
      } else {
        pvaladdr = addr;
      }
    }
    if (0L == *(*nxtfitmaddrs)[dimidx]) {
      pvaladdr = NULL;
    }
  }
}

/**
 * It is invoked by Java side computing service.
 * a step of computation should be handle in a whole by this function.
 * param. vinfos contains all relevant value matrixes for this computation.
 * It could return a set of handlers as results or in-place update the data indicated by value matrixes.
 */
JNIEXPORT
jlongArray JNICALL Java_org_apache_mnemonic_service_computingservice_internal_SortServiceImpl_nperformBubbleSort(JNIEnv* env,
    jobject this, jobjectArray vinfos) {
  jlongArray ret = NULL;
  jsize retsz = 1;
  jsize idx;
  size_t visz;
  struct NValueInfo **nvinfos = constructNValueInfos(env, vinfos, &visz);
  retsz = visz;
  jlong nret[retsz];
  for(idx = 0; idx < retsz; ++idx) {
    nret[idx] = 0L;
  }
  printNValueInfos(nvinfos, visz);
  for(idx = 0; idx < visz; ++idx) {
    printf("-- Value Matrix #%u --\n", idx);
    if (NULL != nvinfos + idx) {
      nret[idx] = handleValueInfo(env, *(nvinfos + idx), valSimpleSortHandler);
    } else {
      printf("NULL\n");
    }
  }

  ret = constructJLongArray(env, nret, retsz);
  destructNValueInfos(nvinfos, visz);
  return ret;
}
