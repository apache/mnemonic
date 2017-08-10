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

#include "org_apache_mnemonic_service_computing_internal_SortServiceImpl.h"

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
jlongArray JNICALL Java_org_apache_mnemonic_service_computing_internal_SortServiceImpl_nperformBubbleSort(JNIEnv* env,
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

#define TO_E(nvi, p) addr_from_java(p)

typedef struct {
  long scan_count;
  long swap_count;
  long noswap_count;
} SortInfo;

long handle1DLongBubbleSort(JNIEnv* env, struct NValueInfo *nvinfo, SortInfo *sortinfo) {
  if (NULL == nvinfo->frames || 0 >= nvinfo->framessz) {
    return 0L;
  }
  register long *itmaddrs = NULL;
  register long *nxtfitmaddrs = NULL;
  register int pendings = 0;
  register long hdls = 0;
  hdls = nvinfo->handler;
  itmaddrs = &nvinfo->handler;
  register long *iatmp;
  register long curoff = nvinfo->frames->nextoff;
  register long curnloff = nvinfo->frames->nlvloff;
//  register long curnlsz = nvinfo->frames->nlvlsz;
  register void *addr = NULL;
  assert(-1L != curoff);
  iatmp = itmaddrs;
  register long *hptr1 = NULL, *hptr2 = NULL;
  register void *pvaladdr = NULL;
  register long *tmpptr = NULL;
  register long tmpval = 0L;
  register long cntscan = 0L, cntswap = 0L, cntnoswap = 0L;

  if (0L == hdls) {
    return 0L;
  }

  do {
    ++cntscan;
    pendings = 0;
    pvaladdr = NULL;

    addr = TO_E(nvinfo, hdls) + curnloff;
    nxtfitmaddrs = (long*)(TO_E(nvinfo, hdls) + curoff);
    pvaladdr = addr;
    hptr2 = itmaddrs;

    while (1) {
      itmaddrs = nxtfitmaddrs;
      hdls = *itmaddrs;
      if (0L == hdls) {
        break;
      }
      addr = TO_E(nvinfo, hdls) + curnloff;
      nxtfitmaddrs = (long*)(TO_E(nvinfo, hdls) + curoff);

      hptr1 = hptr2;
      hptr2 = itmaddrs;
      if (*(long*)pvaladdr > *(long*)addr) {
        tmpptr = nxtfitmaddrs;
        tmpval = *tmpptr;
        *tmpptr = *hptr1;
        *hptr1 = *hptr2;
        *hptr2 = tmpval;
        nxtfitmaddrs = hptr2;
        hptr2 = tmpptr;
        pendings = 1;
        ++cntswap;
      } else {
        pvaladdr = addr;
        ++cntnoswap;
      }

    }

    itmaddrs = iatmp;
    hdls = *iatmp;
  } while (0 != pendings);

  if (NULL != sortinfo) {
    sortinfo->scan_count = cntscan;
    sortinfo->swap_count = cntswap;
    sortinfo->noswap_count = cntnoswap;
  }
  return nvinfo->handler;
}

JNIEXPORT
jlongArray JNICALL Java_org_apache_mnemonic_service_computing_internal_SortServiceImpl_nperform1DLongBubbleSort(
    JNIEnv* env, jobject this, jobjectArray vinfos) {
  jlongArray ret = NULL;
  jsize retsz = 1;
  jsize idx;
  size_t visz;
  SortInfo sortinfo;
  struct NValueInfo **nvinfos = constructNValueInfos(env, vinfos, &visz);
  retsz = visz + 3;
  jlong nret[retsz];
  for(idx = 0; idx < retsz; ++idx) {
    nret[idx] = 0L;
  }
  printNValueInfos(nvinfos, visz);
  assert(1 == visz);

  nret[0] = handle1DLongBubbleSort(env, *nvinfos, &sortinfo);
  nret[1] = sortinfo.scan_count;
  nret[2] = sortinfo.swap_count;
  nret[3] = sortinfo.noswap_count;

  ret = constructJLongArray(env, nret, retsz);
  destructNValueInfos(nvinfos, visz);
  return ret;
}
