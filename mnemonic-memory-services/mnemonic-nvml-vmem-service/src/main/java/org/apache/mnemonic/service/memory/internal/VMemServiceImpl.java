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
import org.apache.mnemonic.service.memory.VolatileMemoryAllocatorService;
import org.apache.mnemonic.resgc.ReclaimContext;
import org.apache.mnemonic.primitives.NativeLibraryLoader;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class VMemServiceImpl implements VolatileMemoryAllocatorService {
  private static boolean nativeLoaded = false;
  static void loadNativeLibrary() {
    try {
      NativeLibraryLoader.loadFromJar("vmemallocator");
    } catch (Exception e) {
      throw new Error(e);
    }
    nativeLoaded = true;
  }

  protected Map<Long, Long> m_info = Collections.synchronizedMap(new HashMap<Long, Long>());

  @Override
  public String getServiceId() {
    return "vmem";
  }

  @Override
  public long init(long capacity, String uri, boolean isnew) {
    if (!nativeLoaded) {
      loadNativeLibrary();
    }
    long ret = ninit(capacity, uri, isnew);
    m_info.put(ret, capacity);
    return ret;
  }

  @Override
  public long adjustCapacity(long id, long reserve) {
    throw new UnsupportedOperationException("Unsupported to reduce capacity of this memory service");
  }

  @Override
  public void close(long id) {
    nclose(id);
  }

  @Override
  public void syncToVolatileMemory(long id, long addr, long length, boolean autodetect) {
    nsync(id, addr, length, autodetect);
  }

  @Override
  public long capacity(long id) {
    return m_info.get(id);
  }

  @Override
  public long allocate(long id, long size, boolean initzero) {
    return nallocate(id, size, initzero);
  }

  @Override
  public long reallocate(long id, long addr, long size, boolean initzero) {
    return nreallocate(id, addr, size, initzero);
  }

  @Override
  public void free(long id, long addr, ReclaimContext rctx) {
    nfree(id, addr);
  }

  @Override
  public ByteBuffer createByteBuffer(long id, long size) {
    return ncreateByteBuffer(id, size);
  }

  @Override
  public ByteBuffer resizeByteBuffer(long id, ByteBuffer bytebuf, long size) {
    return nresizeByteBuffer(id, bytebuf, size);
  }

  @Override
  public void destroyByteBuffer(long id, ByteBuffer bytebuf, ReclaimContext rctx) {
    ndestroyByteBuffer(id, bytebuf);
  }

  @Override
  public ByteBuffer retrieveByteBuffer(long id, long handler) {
    return nretrieveByteBuffer(id, handler);
  }

  @Override
  public long retrieveSize(long id, long handler) {
    return nretrieveSize(id, handler);
  }

  @Override
  public long getByteBufferHandler(long id, ByteBuffer buf) {
    return ngetByteBufferHandler(id, buf);
  }

  @Override
  public void setHandler(long id, long key, long handler) {
    nsetHandler(id, key, handler);
  }

  @Override
  public long getHandler(long id, long key) {
    return ngetHandler(id, key);
  }

  @Override
  public long handlerCapacity(long id) {
    return nhandlerCapacity(id);
  }

  @Override
  public long getBaseAddress(long id) {
    return 0L;
    //return ngetBaseAddress(id);
  }

  @Override
  public void beginTransaction(boolean readOnly) {
    throw new UnsupportedOperationException("Not support transaction");
  }

  @Override
  public void commitTransaction() {
    throw new UnsupportedOperationException("Not support transaction");
  }

  @Override
  public void abortTransaction() {
    throw new UnsupportedOperationException("Not support transaction");
  }

  @Override
  public boolean isInTransaction() {
    throw new UnsupportedOperationException("Not support transaction");
  }

  @Override
  public Set<MemoryServiceFeature> getFeatures() {
    Set<MemoryServiceFeature> ret = new HashSet<MemoryServiceFeature>();
    ret.add(MemoryServiceFeature.VOLATILE);
    return ret;
  }

  @Override
  public byte[] getAbstractAddress(long addr) {
    throw new UnsupportedOperationException("Unrsupported to get abstract address");
  }

  @Override
  public long getPortableAddress(long addr) {
    throw new UnsupportedOperationException("Unrsupported to get portable address");
  }

  @Override
  public long getEffectiveAddress(long addr) {
    throw new UnsupportedOperationException("Unrsupported to get effective address");
  }

  @Override
  public long[] getMemoryFunctions() {
    return new long[0];
  }

  protected native long ninit(long capacity, String uri, boolean isnew);

  protected native void nclose(long id);

  protected native void nsync(long id, long addr, long length, boolean autodetect);

  protected native long ncapacity(long id);

  protected native long nallocate(long id, long size, boolean initzero);

  protected native long nreallocate(long id, long addr, long size, boolean initzero);

  protected native void nfree(long id, long addr);

  protected native ByteBuffer ncreateByteBuffer(long id, long size);

  protected native ByteBuffer nresizeByteBuffer(long id, ByteBuffer bytebuf, long size);

  protected native void ndestroyByteBuffer(long id, ByteBuffer bytebuf);

  protected native ByteBuffer nretrieveByteBuffer(long id, long handler);

  protected native long nretrieveSize(long id, long handler);

  protected native long ngetByteBufferHandler(long id, ByteBuffer buf);

  protected native void nsetHandler(long id, long key, long handler);

  protected native long ngetHandler(long id, long key);

  protected native long nhandlerCapacity(long id);

  protected native long ngetBaseAddress(long id);

  /* Optional Queryable Service */

  @Override
  public String[] getClassNames(long id) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String[] getEntityNames(long id, String clsname) {
    throw new UnsupportedOperationException();
  }

  @Override
  public EntityInfo getEntityInfo(long id, String clsname, String etyname) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void createEntity(long id, EntityInfo entityinfo) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void destroyEntity(long id, String clsname, String etyname) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void updateQueryableInfo(long id, String clsname, String etyname, ValueInfo updobjs) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteQueryableInfo(long id, String clsname, String etyname, ValueInfo updobjs) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResultSet query(long id, String querystr) {
    throw new UnsupportedOperationException();
  }
}
