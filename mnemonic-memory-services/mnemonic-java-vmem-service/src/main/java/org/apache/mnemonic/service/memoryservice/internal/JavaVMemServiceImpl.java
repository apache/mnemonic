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
import org.apache.mnemonic.ConfigurationException;
import org.flowcomputing.commons.primitives.NativeLibraryLoader;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JavaVMemServiceImpl implements VolatileMemoryAllocatorService {

  protected Map<Long, Long> m_info = Collections.synchronizedMap(new HashMap<Long, Long>());
  protected ArrayList<RandomAccessFile> mem_pool = new ArrayList<RandomAccessFile>();

  @Override
  public String getServiceId() {
    return "javavmem";
  }

  @Override
  public long init(long capacity, String uri, boolean isnew) {
    FileChannel channel = null;
    RandomAccessFile mappedFile = null;
    long cp = null;
    long ret = null;


    if (uri == null || uri.length == 0) {
      throw new ConfigurationException(String.format("Please supply the file path: %s.", uri));
    }
    if (capacity <= 0) {
      throw new ConfigurationException("Please supply the capacity");
    }

    File file = new File(uri);

    if (file.exists() && !file.isFile()) {
      throw new ConfigurationException(String.format("Please supply the file path: %s.", uri));
    }
    if (file.isFile() && file.length() <= 0) {
      throw new ConfigurationException("File length should be more than zero.");
    }

    if (isnew) {
      if (file.exists()) {
        if (!file.delete()) {
          throw new ConfigurationException(String.format("Failed to delete the file: %s.", uri));
        }
      }
      mappedFile = new RandomAccessFile(file, "rw");
      mappedFile.setLength(capacity);
    } else {
      mappedFile = new RandomAccessFile(file, "rw");
    }

    cp = file.length();
    mem_pool.add(mappedFile);
    ret = mem_pool.length - 1;
    m_info.put(ret, cp);

    return ret;
  }

  @Override
  public long adjustCapacity(long id, long reserve) {
    throw new UnsupportedOperationException("Unsupported to reduce capacity of this memory service");
  }

  @Override
  public void close(long id) {
    if (mem_pool.get(id) != null) {
      mem_pool.get(id).close();
      mem_pool.get(id) = null;
    }
  }

  @Override
  public void sync(long id, long addr, long length, boolean autodetect) {
    throw new UnsupportedOperationException("Unsupported to synchronization operation");
  }

  @Override
  public long capacity(long id) {
    return m_info.get(id);
  }

  @Override
  public long allocate(long id, long size, boolean initzero) {
    return 1L; //need detail
  }

  @Override
  public long reallocate(long id, long addr, long size, boolean initzero) {
    return 1L; //need detail
  }

  @Override
  public void free(long id, long addr) {
    ///mem_pool.get(id) = null;//need change//allocateVS free
  }

  @Override
  public ByteBuffer createByteBuffer(long id, long size) {
    ByteBuffer myByteBuffer = null;
    /*try {
    MapMode mapMode = readWrite ? MapMode.READ_WRITE : MapMode.READ_ONLY;
    FileChannel channel = mem_pool.get(id).getChannel();
    myByteBuffers = channel.map(mapMode, XXXXX, size);
    } catch (Exception e) {
        myBytebuffers = null;
    }*///need change

    return myByteBuffer;
  }

  @Override
  public ByteBuffer resizeByteBuffer(long id, ByteBuffer bytebuf, long size) {
    ByteBuffer myByteBuffer = null;
    return myByteBuffer; //need change
  }

  @Override
  public void destroyByteBuffer(long id, ByteBuffer bytebuf) {
    //more detail
  }

  @Override
  public ByteBuffer retrieveByteBuffer(long id, long handler) {
    ByteBuffer myByteBuffer = null;
    return myByteBuffer;//need change
  }

  @Override
  public long retrieveSize(long id, long handler) {
    return 1L;//need change
  }

  @Override
  public long getByteBufferHandler(long id, ByteBuffer buf) {
    return 1L;//need change
  }

  @Override
  public void setHandler(long id, long key, long handler) {
    throw new UnsupportedOperationException("Unsupported to set handler");
  }

  @Override
  public long getHandler(long id, long key) {
    throw new UnsupportedOperationException("Unsupported to get handler");
  }

  @Override
  public long handlerCapacity(long id) {
    return 255;
  }

  @Override
  public long getBaseAddress(long id) {
    return 1L;//need change
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
}