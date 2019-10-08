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

#ifndef _LAYOUT_H
#define _LAYOUT_H
#ifdef __cplusplus
extern "C" {
#endif

#define MAX_HANDLER_STORE_LEN 256

struct pmem_root {
  long hdl_buf[MAX_HANDLER_STORE_LEN];
  size_t capacity;
};

POBJ_LAYOUT_BEGIN(memory_service);
POBJ_LAYOUT_ROOT(memory_service, struct pmem_root);
POBJ_LAYOUT_TOID(memory_service, uint8_t);
POBJ_LAYOUT_TOID(memory_service, size_t);
POBJ_LAYOUT_END(memory_service);

typedef struct {
  //size_t size;
  jlong size;
} PMBHeader;

typedef struct {
  PMEMobjpool *pop;
  uint64_t uuid_lo;
  void *base;
  size_t capacity;
} PMPool;

#define PMBHSZ (sizeof(PMBHeader))

#ifdef __cplusplus
}
#endif
#endif
