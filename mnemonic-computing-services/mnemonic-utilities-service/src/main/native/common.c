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

#include <common.h>

#define TRANSTABLEITEMLEN 3
#define FRAMESITEMLEN 4

/******************************************************************************
 ** Generally-useful functions for JNI programming.
 *****************************************************************************/

/**
 *  Throws a RuntimeException, with either an explicit message or the message
 *  corresponding to the current system error value.
 */
void throw(JNIEnv* env, const char* msg) {
  if (msg == NULL)
    msg = strerror(errno);

  jclass xklass = (*env)->FindClass(env, "java/lang/RuntimeException");
  (*env)->ThrowNew(env, xklass, msg);
}

void* addr_from_java(jlong addr) {
  // This assert fails in a variety of ways on 32-bit systems.
  // It is impossible to predict whether native code that converts
  // pointers to longs will sign-extend or zero-extend the addresses.
  //assert(addr == (uintptr_t)addr, "must not be odd high bits");
  return (void*) (uintptr_t) addr;
}

jlong addr_to_java(void* p) {
  assert(p == (void*) (uintptr_t) p);
  return (long) (uintptr_t) p;
}

void destructNValInfo(struct NValueInfo *nvinfo) {
  if (NULL != nvinfo) {
    if (NULL != nvinfo->transtable) {
      free(nvinfo->transtable);
    }
    if (NULL != nvinfo->memfuncs) {
      free(nvinfo->memfuncs);
    }
    if (NULL != nvinfo->frames) {
      free(nvinfo->frames);
    }
    free(nvinfo);
  }
}

struct NValueInfo *constructNValInfo(JNIEnv* env, jobject vinfoobj) {
  struct NValueInfo *ret = NULL;

  static int inited = 0;
  static jclass vinfocls = NULL, dutenum = NULL;
  static jfieldID handler_fid = NULL, transtable_fid = NULL, memfuncs_fid = NULL;
  static jfieldID frames_fid = NULL, dtype_fid = NULL;
  static jmethodID getval_mtd = NULL;

  if (NULL == vinfoobj) {
    return NULL;
  }

  if (0 == inited) {
    inited = 1;
    vinfocls = (*env)->FindClass(env, "org/apache/mnemonic/service/computing/ValueInfo");
    dutenum = (*env)->FindClass(env, "org/apache/mnemonic/DurableType");
    if (NULL != vinfocls && NULL != dutenum) {
      handler_fid = (*env)->GetFieldID(env, vinfocls, "handler", "J");
      transtable_fid = (*env)->GetFieldID(env, vinfocls, "transtable", "[[J");
      memfuncs_fid = (*env)->GetFieldID(env, vinfocls, "memfuncs", "[J");
      frames_fid = (*env)->GetFieldID(env, vinfocls, "frames", "[[J");
      dtype_fid = (*env)->GetFieldID(env, vinfocls, "dtype",
          "Lorg/apache/mnemonic/DurableType;");
      getval_mtd = (*env)->GetMethodID(env, dutenum, "getValue", "()I");
    } else {
      return NULL;
    }
  }

  if (NULL == vinfocls || NULL == handler_fid ||
      NULL == transtable_fid || NULL == memfuncs_fid || NULL == frames_fid ||
      NULL == dtype_fid || NULL == getval_mtd) {
    return NULL;
  }

  ret = (struct NValueInfo *)calloc(1, sizeof(struct NValueInfo));
  if (NULL != ret) {
    jlongArray itmarr;
    jlong *itms;
    jsize itmarrlen, i;
    ret->handler = (*env)->GetLongField(env, vinfoobj, handler_fid);
    jobject dutobj = (*env)->GetObjectField(env, vinfoobj, dtype_fid);
    ret->dtype = (*env)->CallIntMethod(env, dutobj, getval_mtd);

    jobjectArray tbarr = (*env)->GetObjectField(env, vinfoobj, transtable_fid);
    jsize tbarrlen = 0;
    ret->transtablesz = 0;
    if (NULL != tbarr) {
      tbarrlen = (*env)->GetArrayLength(env, tbarr);
    }
    if (tbarrlen > 0){
      ret->transtable = (struct transitem *)calloc(
          tbarrlen, sizeof(struct transitem));
      if (NULL != ret->transtable) {
        ret->transtablesz = tbarrlen;
        for (i = 0; i < tbarrlen; ++i) {
          itmarr = (jlongArray)((*env)->GetObjectArrayElement(env, tbarr, i));
          itmarrlen = (*env)->GetArrayLength(env, itmarr);
          if (NULL != itmarr && TRANSTABLEITEMLEN == itmarrlen) {
            itms = (*env)->GetLongArrayElements(env, itmarr, NULL);
            (ret->transtable + i)->hdlbase = itms[0];
            (ret->transtable + i)->size = itms[1];
            (ret->transtable + i)->base = addr_from_java(itms[2]);
            (*env)->ReleaseLongArrayElements(env, itmarr, itms, JNI_ABORT);
          } else {
            return NULL;
          }
        }
      } else {
        destructNValInfo(ret);
        return NULL;
      }
    }

    jobjectArray mfarr = (*env)->GetObjectField(env, vinfoobj, memfuncs_fid);
    jsize mfarrlen = 0;
    ret->memfuncssz = 0;
    if (NULL != mfarr) {
      mfarrlen = (*env)->GetArrayLength(env, mfarr);
    }
    if (mfarrlen > 0){
      ret->memfuncs = (void* *)calloc(
          mfarrlen, sizeof(jlong));
      if (NULL != ret->memfuncs) {
        ret->memfuncssz = mfarrlen;
        itms = (*env)->GetLongArrayElements(env, mfarr, NULL);
        memcpy(ret->memfuncs, itms, mfarrlen * sizeof(jlong));
        (*env)->ReleaseLongArrayElements(env, mfarr, itms, JNI_ABORT);
      } else {
        destructNValInfo(ret);
        return NULL;
      }
    }

    jobjectArray fmarr = (*env)->GetObjectField(env, vinfoobj, frames_fid);
    jsize fmarrlen = (*env)->GetArrayLength(env, fmarr);
    if (NULL != fmarr && fmarrlen > 0){
      ret->frames = (struct frameitem *)calloc(
          fmarrlen, sizeof(struct frameitem));
      if (NULL != ret->frames) {
        ret->framessz = fmarrlen;
        for (i = 0; i < fmarrlen; ++i) {
          itmarr = (jlongArray)((*env)->GetObjectArrayElement(env, fmarr, i));
          itmarrlen = (*env)->GetArrayLength(env, itmarr);
          if (NULL != itmarr && FRAMESITEMLEN == itmarrlen) {
            itms = (*env)->GetLongArrayElements(env, itmarr, NULL);
            (ret->frames + i)->nextoff = itms[0];
            (ret->frames + i)->nextsz = itms[1];
            (ret->frames + i)->nlvloff = itms[2];
            (ret->frames + i)->nlvlsz = itms[3];
            (*env)->ReleaseLongArrayElements(env, itmarr, itms, JNI_ABORT);
          } else {
            return NULL;
          }
        }
      } else {
        destructNValInfo(ret);
        return NULL;
      }
    }
  } else {
    return NULL;
  }
  return ret;
}

struct NValueInfo **constructNValueInfos(JNIEnv* env,
    jobjectArray vinfos, size_t *sz) {
  if (NULL == sz) {
    return NULL;
  }
  size_t idx;
  struct NValueInfo **ret = NULL;
  struct NValueInfo *curnvi = NULL;
  jobject curviobj;

  *sz = (*env)->GetArrayLength(env, vinfos);
  if (0 >= *sz) {
    return NULL;
  }
  ret = (struct NValueInfo **)calloc(*sz, sizeof(struct NValueInfo*));
  if (NULL == ret) {
    return NULL;
  }

  for(idx = 0; idx < *sz; ++idx) {
    curviobj = (*env)->GetObjectArrayElement(env, vinfos, idx);
    if (NULL == curviobj) {
      continue;
    }
    curnvi = constructNValInfo(env, curviobj);
    if (NULL == curnvi) {
      destructNValueInfos(ret, *sz);
      return NULL;
    }
    *(ret + idx) = curnvi;
  }

  return ret;
}

void destructNValueInfos(struct NValueInfo **nvalinfos, size_t sz) {
  size_t idx;
  if (NULL != nvalinfos) {
    for(idx = 0; idx < sz; ++idx) {
      destructNValInfo(*(nvalinfos + idx));
    }
    free(nvalinfos);
  }
}

void printNValueInfos(struct NValueInfo **nvalinfos, size_t sz) {
  size_t idx, i;
  struct NValueInfo *itm;
  struct transitem *ttitm;
  struct frameitem *fitm;
  printf("\n--- Native ValueInfo List, Addr:%p, Size: %zu ---\n", nvalinfos, sz);
  if (NULL != nvalinfos) {
    for(idx = 0; idx < sz; ++idx) {
      itm = *(nvalinfos + idx);
      printf("** Item %2zu, Addr:%p ** \n", idx, itm);
      if (NULL != itm) {
        printf("Handler:%ld, DType Value:%d\n", itm->handler, itm->dtype);
        printf(">> TransTable: Addr:%p, Size:%zu <<\n", itm->transtable, itm->transtablesz);
        if (NULL != itm->transtable && itm->transtablesz > 0) {
          for (i = 0; i < itm->transtablesz; ++i) {
            ttitm = (itm->transtable + i);
            printf("%2zu)", i);
            if (NULL != ttitm) {
              printf("hdlbase:%ld, size:%ld, base:%p\n",
                  ttitm->hdlbase, ttitm->size, ttitm->base);
            } else {
              printf("NULL\n");
            }
          }
        } else {
          printf("NULL\n");
        }
        printf(">> Frames: Addr:%p, Size:%zu <<\n", itm->frames, itm->framessz);
        if (NULL != itm->frames && itm->framessz > 0) {
          for (i = 0; i < itm->framessz; ++i) {
            fitm = (itm->frames + i);
            printf("%2zu)", i);
            if (NULL != fitm) {
              printf("nextoff:%ld, nextsz:%ld, nlvloff:%ld, nlvlsz:%ld\n",
                  fitm->nextoff, fitm->nextsz, fitm->nlvloff, fitm->nlvlsz);
            } else {
              printf("NULL\n");
            }
          }
        } else {
          printf("NULL\n");
        }
      } else {
        printf("NULL\n");
      }
    }
  } else {
    printf("NULL\n");
  }
  printf("------------------------\n");
}

jlongArray constructJLongArray(JNIEnv* env, long arr[], size_t sz) {
  jlongArray ret = (*env)->NewLongArray(env, sz);
  if (NULL == ret) {
    return NULL;
  }
  (*env)->SetLongArrayRegion(env, ret, 0, sz, arr);
  return ret;
}

inline void *to_e(JNIEnv* env, struct NValueInfo *nvinfo, long p) {
  size_t i;
  struct transitem * ti;
  if (NULL != nvinfo && NULL != nvinfo->transtable) {
    for (i = 0; i < nvinfo->transtablesz; ++i) {
      ti = nvinfo->transtable + i;
      if (p >= ti->hdlbase && p < ti->size) {
        return ti->base + p;
      }
    }
    throw(env, "No item found in Translate Table.");
  } else {
    return addr_from_java(p);
  }
  return NULL;
}

inline long to_p(JNIEnv* env, struct NValueInfo *nvinfo, void *e) {
  size_t i;
  struct transitem * ti;
  if (NULL != nvinfo && NULL != nvinfo->transtable) {
    for (i = 0; i < nvinfo->transtablesz; ++i) {
      ti = nvinfo->transtable + i;
      if (e >= ti->base && e < ti->base + ti->size) {
        return e - ti->base;
      }
    }
    throw(env, "No item found in Translate Table.");
  } else {
    return addr_to_java(e);
  }
  return -1L;
}

void iterTensor(JNIEnv* env, struct NValueInfo *nvinfo,
    size_t dims[], long *itmaddrs[], long *(* const nxtfitmaddrs)[],
    long (* const pendings)[], long hdls[],
    size_t dimidx, valueHandler valhandler) {
  if (++dimidx >= nvinfo->framessz) {
    return;
  }
  long *iatmp;
  long curoff = (nvinfo->frames + dimidx)->nextoff;
  long curnloff = (nvinfo->frames + dimidx)->nlvloff;
  long curnlsz = (nvinfo->frames + dimidx)->nlvlsz;
  void *addr = NULL;
  if (dimidx < nvinfo->framessz - 1) {
    iatmp = itmaddrs[dimidx];
    while (1) {
      (*pendings)[dimidx] = 0L;
      while(0L != hdls[dimidx]) {
        itmaddrs[dimidx + 1] = (long*)(to_e(env, nvinfo, hdls[dimidx]) + curnloff);
        hdls[dimidx + 1] = *itmaddrs[dimidx + 1];
        (*nxtfitmaddrs)[dimidx] = (long*)(to_e(env, nvinfo, hdls[dimidx]) + curoff);
        iterTensor(env, nvinfo, dims, itmaddrs, nxtfitmaddrs, pendings, hdls, dimidx, valhandler);
        itmaddrs[dimidx] = (*nxtfitmaddrs)[dimidx];
        hdls[dimidx] = *itmaddrs[dimidx];
        ++dims[dimidx];
      }
      dims[dimidx] = 0;
      if (0L == (*pendings)[dimidx]) {
        break;
      }
      itmaddrs[dimidx] = iatmp;
      hdls[dimidx] = *iatmp;
    }
  } else {
    if (-1L != curoff) {
      iatmp = itmaddrs[dimidx];
      while (1) {
        (*pendings)[dimidx] = 0L;
        while(0L != hdls[dimidx]) {
          addr = to_e(env, nvinfo, hdls[dimidx]) + curnloff;
          (*nxtfitmaddrs)[dimidx] = (long*)(to_e(env, nvinfo, hdls[dimidx]) + curoff);
          valhandler(env, dims, dimidx, itmaddrs, nxtfitmaddrs, pendings, addr, curnlsz, nvinfo->dtype);
          itmaddrs[dimidx] = (*nxtfitmaddrs)[dimidx];
          hdls[dimidx] = *itmaddrs[dimidx];
          ++dims[dimidx];
        }
        dims[dimidx] = 0;
        if (0L == (*pendings)[dimidx]) {
          break;
        }
        itmaddrs[dimidx] = iatmp;
        hdls[dimidx] = *iatmp;
      }
    } else {
      addr = to_e(env, nvinfo, hdls[dimidx]) + curnloff;
      valhandler(env, dims, dimidx - 1, itmaddrs, nxtfitmaddrs, pendings, addr, curnlsz, nvinfo->dtype);
    }
  }
}

long handleValueInfo(JNIEnv* env, struct NValueInfo *nvinfo, valueHandler valhandler) {
  if (NULL == nvinfo->frames || 0 >= nvinfo->framessz) {
    return 0L;
  }
  size_t dims[nvinfo->framessz];
  long *itmaddrs[nvinfo->framessz];
  long *nxtfitmaddrs[nvinfo->framessz];
  long pendings[nvinfo->framessz];
  long hdls[nvinfo->framessz];
  size_t i;
  for (i = 0; i < nvinfo->framessz; ++i) {
    dims[i] = 0;
    itmaddrs[i] = NULL;
    nxtfitmaddrs[i] = NULL;
    pendings[i] = 0L;
    hdls[i] = 0L;
  }
  hdls[0] = nvinfo->handler;
  itmaddrs[0] = &nvinfo->handler;
  iterTensor(env, nvinfo, dims, itmaddrs, &nxtfitmaddrs, &pendings, hdls, -1, valhandler);
  return nvinfo->handler;
}

void vectorIterTensor(JNIEnv* env, struct NValueInfo *nvinfo,
    size_t dims[], long *itmaddrs[], long *(* const nxtfitmaddrs)[],
    long (* const pendings)[], long hdls[],
    size_t dimidx, vecValueHandler valhandler, void *chkaddr, long chksize, long *vposition, long *vcount) {
  if (++dimidx >= nvinfo->framessz) {
    return;
  }
  long *iatmp;
  long curoff = (nvinfo->frames + dimidx)->nextoff;
  long curnloff = (nvinfo->frames + dimidx)->nlvloff;
  long curnlsz = (nvinfo->frames + dimidx)->nlvlsz;
  void *addr = NULL;
  if (dimidx < nvinfo->framessz - 1) {
    iatmp = itmaddrs[dimidx];
    while (1) {
      (*pendings)[dimidx] = 0L;
      while(0L != hdls[dimidx]) {
        itmaddrs[dimidx + 1] = (long*)(to_e(env, nvinfo, hdls[dimidx]) + curnloff);
        hdls[dimidx + 1] = *itmaddrs[dimidx + 1];
        (*nxtfitmaddrs)[dimidx] = (long*)(to_e(env, nvinfo, hdls[dimidx]) + curoff);
        vectorIterTensor(env, nvinfo, dims, itmaddrs, nxtfitmaddrs, pendings, hdls, dimidx, valhandler, chkaddr, chksize, vposition, vcount);
        itmaddrs[dimidx] = (*nxtfitmaddrs)[dimidx];
        hdls[dimidx] = *itmaddrs[dimidx];
        ++dims[dimidx];
      }
      dims[dimidx] = 0;
      if (0L == (*pendings)[dimidx]) {
        break;
      }
      itmaddrs[dimidx] = iatmp;
      hdls[dimidx] = *iatmp;
    }
  } else {
    if (-1L != curoff) {
      iatmp = itmaddrs[dimidx];
      while (1) {
        (*pendings)[dimidx] = 0L;
        while(0L != hdls[dimidx]) {
          addr = to_e(env, nvinfo, hdls[dimidx]) + curnloff;
          (*nxtfitmaddrs)[dimidx] = (long*)(to_e(env, nvinfo, hdls[dimidx]) + curoff);
          valhandler(env, dims, dimidx, itmaddrs, nxtfitmaddrs, pendings, addr, curnlsz, nvinfo->dtype, chkaddr, chksize, vposition, vcount);
          itmaddrs[dimidx] = (*nxtfitmaddrs)[dimidx];
          hdls[dimidx] = *itmaddrs[dimidx];
          ++dims[dimidx];
        }
        dims[dimidx] = 0;
        if (0L == (*pendings)[dimidx]) {
          break;
        }
        itmaddrs[dimidx] = iatmp;
        hdls[dimidx] = *iatmp;
      }
    } else {
      addr = to_e(env, nvinfo, hdls[dimidx]) + curnloff;
      valhandler(env, dims, dimidx - 1, itmaddrs, nxtfitmaddrs, pendings, addr, curnlsz, nvinfo->dtype, chkaddr, chksize, vposition, vcount);
    }
  }
}

long handleVectorInfo(JNIEnv* env, struct NValueInfo *nvinfo, vecValueHandler valhandler, long dc_handler, long dc_size, long* count) {
  if (NULL == nvinfo->frames || 0 >= nvinfo->framessz) {
    return 0L;
  }
  void *chk_addr = to_e(env, nvinfo, dc_handler); 
  size_t dims[nvinfo->framessz];
  long *itmaddrs[nvinfo->framessz];
  long *nxtfitmaddrs[nvinfo->framessz];
  long pendings[nvinfo->framessz];
  long hdls[nvinfo->framessz];
  long position = 0;
  size_t i;
  for (i = 0; i < nvinfo->framessz; ++i) {
    dims[i] = 0;
    itmaddrs[i] = NULL;
    nxtfitmaddrs[i] = NULL;
    pendings[i] = 0L;
    hdls[i] = 0L;
  }
  hdls[0] = nvinfo->handler;
  itmaddrs[0] = &nvinfo->handler;
  vectorIterTensor(env, nvinfo, dims, itmaddrs, &nxtfitmaddrs, &pendings, hdls, -1, valhandler, chk_addr, dc_size, &position, count);
  return nvinfo->handler;
}
