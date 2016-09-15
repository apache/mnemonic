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

package org.apache.mnemonic.service.memoryservice.internal;

import org.apache.mnemonic.service.memoryservice.VolatileMemoryAllocatorService;
import org.flowcomputing.commons.primitives.NativeLibraryLoader;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class VMemServiceImpl implements VolatileMemoryAllocatorService {
  static {
    try {
      NativeLibraryLoader.loadFromJar("vmemallocator");
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  protected Map<Long, Long> m_info = Collections.synchronizedMap(new HashMap<Long, Long>());

  @Override
  public String getServiceId() {
    return "vmem";
  }

  @Override
  public long init(long capacity, String uri, boolean isnew) {
    long ret = ninit(capacity, uri, isnew);
    m_info.put(ret, capacity);
    return ret;
  }

  @Override
  public void close(long id) {
    nclose(id);
  }

  @Override
  public void sync(long id) {
    nsync(id);
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
  public long reallocate(long id, long address, long size, boolean initzero) {
    return nreallocate(id, address, size, initzero);
  }

  @Override
  public void free(long id, long address) {
    nfree(id, address);
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
  public void destroyByteBuffer(long id, ByteBuffer bytebuf) {
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
    return ngetBaseAddress(id);
  }

  protected native long ninit(long capacity, String uri, boolean isnew);

  protected native void nclose(long id);

  protected native void nsync(long id);

  protected native long ncapacity(long id);

  protected native long nallocate(long id, long size, boolean initzero);

  protected native long nreallocate(long id, long address, long size, boolean initzero);

  protected native void nfree(long id, long address);

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

}
