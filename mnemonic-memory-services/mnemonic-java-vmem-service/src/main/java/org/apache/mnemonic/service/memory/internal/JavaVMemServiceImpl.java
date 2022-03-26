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


import org.apache.mnemonic.ConfigurationException;
import org.apache.mnemonic.query.memory.EntityInfo;
import org.apache.mnemonic.query.memory.ResultSet;
import org.apache.mnemonic.service.computing.ValueInfo;
import org.apache.mnemonic.service.memory.MemoryServiceFeature;
import org.apache.mnemonic.service.memory.VolatileMemoryAllocatorService;
import org.apache.mnemonic.resgc.ReclaimContext;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Set;


public class JavaVMemServiceImpl implements VolatileMemoryAllocatorService {

  protected static final int CHUNK_BLOCK_SIZE = 512;
  protected static final MapMode MAP_MODE = MapMode.READ_WRITE;
  protected static final int MAX_BUFFER_BLOCK_SIZE = Integer.MAX_VALUE;

  protected ArrayList<MemoryInfo> mem_pools = new ArrayList<MemoryInfo>();

  @Override
  public String getServiceId() {
    return "javavmem";
  }

  @Override
  public long init(long capacity, String uri, boolean isnew) {
    FileChannel channel = null;
    RandomAccessFile mappedFile = null;
    long cp = -1;
    long ret = -1;

    if (uri == null || uri.length() == 0) {
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
      try {
        mappedFile = new RandomAccessFile(file, "rw");
        mappedFile.setLength(capacity);
        cp = mappedFile.length();
      } catch (Exception ex) { }
    } else {
      if (!file.exists()) {
        throw new ConfigurationException(String.format("File doesn't exist under the specifiled uri: %s", uri));
      }
      try {
        mappedFile = new RandomAccessFile(file, "rw");
        cp = mappedFile.length();
      } catch (Exception ex) { }
    }


    if (mappedFile != null) {
      FileChannel fc = mappedFile.getChannel();
      MemoryInfo mi = new MemoryInfo();
      mi.setMemCapacity(cp);
      mi.setRandomAccessFile(mappedFile);
      mi.setFileChannel(fc);


      if (cp <= MAX_BUFFER_BLOCK_SIZE) {
        mi.setByteBufferBlocksList(createBufferBlockInfo(fc, 0L, (int)cp));
      } else {
        int bid;
        for (bid = 0; bid < cp / MAX_BUFFER_BLOCK_SIZE; bid++) {
          mi.setByteBufferBlocksList(
                  createBufferBlockInfo(fc, bid * MAX_BUFFER_BLOCK_SIZE, MAX_BUFFER_BLOCK_SIZE));
        }
        if (cp % MAX_BUFFER_BLOCK_SIZE > 0) {
          mi.setByteBufferBlocksList(
                  createBufferBlockInfo(fc, bid * MAX_BUFFER_BLOCK_SIZE, (int)cp - MAX_BUFFER_BLOCK_SIZE * bid));
        }
      }

      getMemPools().add(mi);
      ret = this.getMemPools().size() - 1;

     }

    return ret;
  }

  @Override
  public long adjustCapacity(long id, long reserve) {
    throw new UnsupportedOperationException("Unsupported to reduce capacity of this memory service");
  }

  @Override
  public void close(long id) {
    int idx = (int) id;
    MemoryInfo mi = this.getMemPools().get(idx);
    if (mi.getFileChannel() != null) {
      try {
        mi.getFileChannel().close();
      } catch (Exception e) {
      } finally {
        mi.setFileChannel(null);
      }
    }
    if (mi.getRandomAccessFile() != null) {
      try {
        mi.getRandomAccessFile().close();
      } catch (Exception e) {
      } finally {
        mi.setRandomAccessFile(null);
      }
    }
  }

  @Override
  public void syncToVolatileMemory(long id, long addr, long length, boolean autodetect) {
    throw new UnsupportedOperationException("Unsupported to synchronization operation");
  }

  @Override
  public long capacity(long id) {
    MemoryInfo mi = this.getMemPools().get((int)id);
    return mi.getMemCapacity();
  }

  @Override
  public long allocate(long id, long size, boolean initzero) {
    ByteBuffer bb = null;
    long handler = 0L;
    int bufSize = (int)size;
    MemoryInfo mi = this.getMemPools().get((int)id);
    FileChannel fc = mi.getFileChannel();
    ArrayList<BufferBlockInfo> bufferBlockInfo = mi.getByteBufferBlocksList();
    int requiredBlocks = (int)Math.ceil((double)bufSize / CHUNK_BLOCK_SIZE);

    if (size > MAX_BUFFER_BLOCK_SIZE) {
      throw new ConfigurationException("Buffer size should be less than 2G.");
    }
    for (int blockIdx = 0; blockIdx < bufferBlockInfo.size(); blockIdx++) {
      BitSet chunksMap = bufferBlockInfo.get(blockIdx).bufferBlockChunksMap;
      int startIdx = findStartIdx(chunksMap, requiredBlocks);
      if (startIdx > -1) {
        handler = bufferBlockInfo.get(blockIdx).getBufferBlockBaseAddress() + startIdx * CHUNK_BLOCK_SIZE;
        bb = createChunkBuffer(handler, bufSize);
        if (initzero) {
          for (int i = 0; i < bufSize; i++) {
            bb.put((byte)0);
          }
        }
        markUsed(chunksMap, startIdx, requiredBlocks);
        BufferBlockInfo bufferBlock = mi.getByteBufferBlocksList().get(blockIdx);
        if (bufferBlock != null) {
          bufferBlock.setChunkSizeMap(handler, bufSize);
        }
        break;
      }
    }

    return handler;
  }

  @Override
  public long reallocate(long id, long addr, long size, boolean initzero) {
    long handler = 0L;
    ByteBuffer originalBytebuf = retrieveByteBuffer(id, addr);
    ByteBuffer newBytebuf = createByteBuffer(id, size);
    if (initzero) {
      for (int i = 0; i < (int)size; i++) {
        newBytebuf.put((byte)0);
      }
    }
    originalBytebuf.rewind();
    newBytebuf.rewind();
    newBytebuf.put(originalBytebuf);
    destroyByteBuffer(id, originalBytebuf, null);
    handler = getByteBufferHandler(id, newBytebuf);
    return handler;
  }

  @Override
  public void free(long id, long addr, ReclaimContext rctx) {
    MemoryInfo mi = this.getMemPools().get((int)id);
    // FileChannel channel = mi.getFileChannel();
    int startIdx, requiredblocks, size;
    long baseAddr;
    try {
      for (int blockIdx = 0; blockIdx < mi.getByteBufferBlocksList().size(); blockIdx++) {
        BufferBlockInfo bufferBlock = mi.getByteBufferBlocksList().get(blockIdx);
        if (bufferBlock != null && bufferBlock.getChunkSizeMap().containsKey(addr)) {
          size = bufferBlock.getChunkSizeMap().get(addr);
          BitSet chunksMap = bufferBlock.getBufferBlockChunksMap();
          baseAddr = bufferBlock.getBufferBlockBaseAddress();
          startIdx = (int)Math.floor((double)((addr - baseAddr) / CHUNK_BLOCK_SIZE));
          requiredblocks = (int)Math.ceil((double)size / CHUNK_BLOCK_SIZE);
          markFree(chunksMap, startIdx, requiredblocks);
          bufferBlock.getChunkSizeMap().remove(addr);
          break;
        }
      }
    } catch (Exception e) {
    }
  }

  @Override
  public ByteBuffer createByteBuffer(long id, long size) {
    ByteBuffer bb = null;
    MemoryInfo mi = this.getMemPools().get((int)id);
    FileChannel fc = mi.getFileChannel();
    ArrayList<BufferBlockInfo> bufferBlockInfo = mi.getByteBufferBlocksList();
    int bufSize = (int)size;
    int requiredBlocks = (int)Math.ceil((double)bufSize / CHUNK_BLOCK_SIZE);
    long startAddress;

    if (size > MAX_BUFFER_BLOCK_SIZE) {
      throw new ConfigurationException("Buffer size should be less than 2G.");
    }
    for (int blockIdx = 0; blockIdx < bufferBlockInfo.size(); blockIdx++) {
      BitSet chunksMap = bufferBlockInfo.get(blockIdx).bufferBlockChunksMap;
      int startIdx = findStartIdx(chunksMap, requiredBlocks);
      if (startIdx > -1) {
        startAddress = bufferBlockInfo.get(blockIdx).getBufferBlockBaseAddress() + startIdx * CHUNK_BLOCK_SIZE;
        bb = createChunkBuffer(startAddress, bufSize);
        markUsed(chunksMap, startIdx, requiredBlocks);
        mi.getByteBufferBlocksList().get(blockIdx).setChunkSizeMap(startAddress, bufSize);
        break;
      }
    }

    return bb;
  }

  @Override
  public ByteBuffer resizeByteBuffer(long id, ByteBuffer bytebuf, long size) {
    ByteBuffer bb = createByteBuffer(id, size);
    bytebuf.rewind();
    bb.put(bytebuf);
    destroyByteBuffer(id, bytebuf, null);
    return bb;
  }

  @Override
  public void destroyByteBuffer(long id, ByteBuffer bytebuf, ReclaimContext rctx) {
    MemoryInfo mi = this.getMemPools().get((int)id);
    FileChannel channel = mi.getFileChannel();
    int startIdx, requiredblocks;
    long handler, baseAddr;
    try {
      handler = getByteBufferHandler(id, bytebuf);
      for (int blockIdx = 0; blockIdx < mi.getByteBufferBlocksList().size(); blockIdx++) {
        BufferBlockInfo bufferBlock = mi.getByteBufferBlocksList().get(blockIdx);
        if (bufferBlock != null && bufferBlock.getChunkSizeMap().containsKey(handler)) {
          BitSet chunksMap = bufferBlock.getBufferBlockChunksMap();
          baseAddr = bufferBlock.getBufferBlockBaseAddress();
          startIdx = (int)Math.floor((double)((handler - baseAddr) / CHUNK_BLOCK_SIZE));
          requiredblocks = (int)Math.ceil((double)bytebuf.capacity() / CHUNK_BLOCK_SIZE);
          markFree(chunksMap, startIdx, requiredblocks);
          bufferBlock.getChunkSizeMap().remove(handler);
          break;
        }
      }
    } catch (Exception e) {
    }
  }

  @Override
  public ByteBuffer retrieveByteBuffer(long id, long handler) {
    ByteBuffer bb = null;
    MemoryInfo mi = this.getMemPools().get((int)id);
    int size;
    for (int blockIdx = 0; blockIdx < mi.getByteBufferBlocksList().size(); blockIdx++) {
      BufferBlockInfo blockInfo = mi.getByteBufferBlocksList().get(blockIdx);
      if (blockInfo != null && blockInfo.getChunkSizeMap().containsKey(handler)) {
        size = blockInfo.getChunkSizeMap().get(handler);
        bb = createChunkBuffer(handler, size);
      }
    }

    return bb;
  }

  @Override
  public long retrieveSize(long id, long handler) {
    int size = 0;
    MemoryInfo mi = this.getMemPools().get((int)id);
    for (int blockIdx = 0; blockIdx < mi.getByteBufferBlocksList().size(); blockIdx++) {
      BufferBlockInfo blockInfo = mi.getByteBufferBlocksList().get(blockIdx);
      if (blockInfo != null && blockInfo.getChunkSizeMap().containsKey(handler)) {
        size = blockInfo.getChunkSizeMap().get(handler);
      }
    }
    return size;
  }

  @Override
  public long getByteBufferHandler(long id, ByteBuffer buf) {
    long handler = 0L;
    try {
      Field addressField = Buffer.class.getDeclaredField("address");
      addressField.setAccessible(true);
      handler = addressField.getLong(buf);
    } catch (NoSuchFieldException e) {
      throw new ConfigurationException("Buffer fields not found.");
    } catch (IllegalAccessException e) {
      throw new ConfigurationException("Buffer fields cannot be accessed.");
    }
    return handler;
  }

  @Override
  public void setHandler(long id, long key, long handler) {
    throw new UnsupportedOperationException("Unrsupported to set handler");
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
    throw new UnsupportedOperationException("Not support transaction");
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
    return null;
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

  public ArrayList<MemoryInfo> getMemPools() {
    return this.mem_pools;
  }

  public int findStartIdx(BitSet blocksMap, int requiredBlocks) {
    int startIdx = -1, blocks = 0, idx = 0;
    for (idx = 0; idx < blocksMap.size(); ++idx) {
      if (!blocksMap.get(idx)) {
         blocks++;
      } else {
         blocks = 0;
      }
      if (blocks == requiredBlocks) {
        break;
      }
    }
    if (blocks == requiredBlocks) {
      startIdx = idx - blocks + 1;
    }
    return startIdx;
  }

  protected void markUsed(BitSet blocksMap, int startIdx, int requiredBlocks) {
    for (int idx = startIdx; idx < (startIdx + requiredBlocks); idx++) {
      blocksMap.set(idx);
    }
  }

  protected void markFree(BitSet blocksMap, int startIdx, int requiredBlocks) {
    for (int idx = startIdx; idx < (startIdx + requiredBlocks); idx++) {
      blocksMap.clear(idx);
    }
  }

  protected BufferBlockInfo createBufferBlockInfo(FileChannel fc, long position, int blockSize) {
    BufferBlockInfo bufferBlockInfo = new BufferBlockInfo();
    try {
      ByteBuffer bb = fc.map(MAP_MODE, position, blockSize);
      if (bufferBlockInfo.getBufferBlockBaseAddress() == 0L) {
        bufferBlockInfo.setBufferBlockBaseAddress(getByteBufferHandler(0L, bb));
      }
      bufferBlockInfo.setBufferBlockSize(blockSize);
      bufferBlockInfo.setBufferBlock(bb);
      int chunkBlockQty = (int)Math.ceil((double)blockSize / CHUNK_BLOCK_SIZE);
      BitSet chunksMap = new BitSet(chunkBlockQty);
      bufferBlockInfo.setBufferBlockChunksMap(chunksMap);
    } catch (IOException e) {
    }

    return bufferBlockInfo;
  }

  protected ByteBuffer createChunkBuffer(long handler, int size) {
    ByteBuffer bb = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder());
    Field address, capacity;
    try {
      address = Buffer.class.getDeclaredField("address");
      address.setAccessible(true);
      capacity = Buffer.class.getDeclaredField("capacity");
      capacity.setAccessible(true);
      address.setLong(bb, handler);
      capacity.setInt(bb, size);
      bb.limit(size);
    } catch (NoSuchFieldException e) {
      throw new ConfigurationException("Buffer fields not found.");
    } catch (IllegalAccessException e) {
      throw new ConfigurationException("Buffer fields cannot be accessed.");
    }
    return bb;
  }

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
