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

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class BufferBlockInfo {
    long bufferBlockBaseAddress = 0L;
    int bufferBlockSize;
    ByteBuffer bufferBlock = null;

    BitSet bufferBlockChunksMap = null;
    Map<Long, Integer> chunkSizeMap = new HashMap<>();

    public ByteBuffer getBufferBlock() {
        return bufferBlock;
    }

    public void setBufferBlock(ByteBuffer byteBufferBlock) {
        this.bufferBlock = byteBufferBlock;
    }

    public BitSet getBufferBlockChunksMap() {
        return bufferBlockChunksMap;
    }

    public void setBufferBlockChunksMap(BitSet chunksMap) {
        this.bufferBlockChunksMap = chunksMap;
    }

    public long getBufferBlockBaseAddress() {
        return bufferBlockBaseAddress;
    }

    public void setBufferBlockBaseAddress(long bufferBlockBaseAddress) {
        this.bufferBlockBaseAddress = bufferBlockBaseAddress;
    }

    public int getBufferBlockSize() {
        return bufferBlockSize;
    }

    public void setBufferBlockSize(int blockSize) {
        this.bufferBlockSize = blockSize;
    }

    public Map<Long, Integer> getChunkSizeMap() {
        return chunkSizeMap;
    }

    public void setChunkSizeMap(long chunkHandler, int chunkSize) {
        chunkSizeMap.put(chunkHandler, chunkSize);
    }



}
