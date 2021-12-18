/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mnemonic.service.memory.internal;

import org.apache.mnemonic.query.memory.EntityInfo;
import org.apache.mnemonic.query.memory.ResultSet;
import org.apache.mnemonic.service.computing.ValueInfo;
import org.apache.mnemonic.service.memory.MemoryServiceFeature;
import org.apache.mnemonic.service.memory.NonVolatileMemoryAllocatorService;
import org.apache.mnemonic.resgc.ReclaimContext;

import java.nio.ByteBuffer;
import java.util.Set;

import redis.clients.jedis.Jedis;

public class RedisServiceImpl implements NonVolatileMemoryAllocatorService {

  @Override
  public String getServiceId() {
    return "redis";
  }

  @Override
  public long init(long capacity, String uri, boolean isnew) {
    return -1;
  }

  @Override
  public long adjustCapacity(long id, long reserve) {
    return -1;
  }

  @Override
  public void close(long id) {
  }

  @Override
  public void syncToVolatileMemory(long id, long addr, long length, boolean autodetect) {
  }

  @Override
  public long capacity(long id) {
    return -1;
  }

  @Override
  public long allocate(long id, long size, boolean initzero) {
    return -1;
  }

  @Override
  public long reallocate(long id, long addr, long size, boolean initzero) {
    return -1;
  }

  @Override
  public void free(long id, long addr, ReclaimContext rctx) {
  }

  @Override
  public ByteBuffer createByteBuffer(long id, long size) {
    return null;
  }

  @Override
  public ByteBuffer resizeByteBuffer(long id, ByteBuffer bytebuf, long size) {
    return null;
  }

  @Override
  public void destroyByteBuffer(long id, ByteBuffer bytebuf, ReclaimContext rctx) {
  }

  @Override
  public ByteBuffer retrieveByteBuffer(long id, long handler) {
    return null;
  }

  @Override
  public long retrieveSize(long id, long handler) {
    return -1;
  }

  @Override
  public long getByteBufferHandler(long id, ByteBuffer buf) {
    return -1;
  }

  @Override
  public void setHandler(long id, long key, long handler) {
  }

  @Override
  public long getHandler(long id, long key) {
    return -1;
  }

  @Override
  public long handlerCapacity(long id) {
    return -1;
  }

  @Override
  public void syncToNonVolatileMemory(long id, long addr, long length, boolean autodetect) {
  }

  @Override
  public void syncToLocal(long id, long addr, long length, boolean autodetect) {
  }

  @Override
  public void drain(long id) {
  }

  @Override
  public long getBaseAddress(long id) {
    return -1;
  }

  @Override
  public void beginTransaction(boolean readOnly) {
  }

  @Override
  public void commitTransaction() {
  }

  @Override
  public void abortTransaction() {
  }

  @Override
  public boolean isInTransaction() {
    return false;
  }

  @Override
  public Set<MemoryServiceFeature> getFeatures() {
    return null;
  }

  @Override
  public byte[] getAbstractAddress(long addr) {
    return null;
  }

  @Override
  public long getPortableAddress(long addr) {
    return -1;
  }

  @Override
  public long getEffectiveAddress(long addr) {
    return -1;
  }

  @Override
  public long[] getMemoryFunctions() {
    return null;
  }

  /* Optional Queryable Service */

  @Override
  public String[] getClassNames(long id) {
    return null;
  }

  @Override
  public String[] getEntityNames(long id, String clsname) {
    return null;
  }

  @Override
  public EntityInfo getEntityInfo(long id, String clsname, String etyname) {
    return null;
  }

  @Override
  public void createEntity(long id, EntityInfo entityinfo) {
  }

  @Override
  public void destroyEntity(long id, String clsname, String etyname) {
  }

  @Override
  public void updateQueryableInfo(long id, String clsname, String etyname, ValueInfo updobjs) {
  }

  @Override
  public void deleteQueryableInfo(long id, String clsname, String etyname, ValueInfo updobjs) {
  }

  @Override
  public ResultSet query(long id, String querystr) {
    return null;
  }

  public void jedisTest() {
    Jedis jedis = new Jedis("localhost");
    jedis.set("foo", "bar");
    String value = jedis.get("foo");
  }
}
