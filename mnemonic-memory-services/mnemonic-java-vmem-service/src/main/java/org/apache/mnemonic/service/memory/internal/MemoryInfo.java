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

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class MemoryInfo {

    long memCapacity;
    FileChannel channel = null;
    RandomAccessFile rdmFile = null;
    ArrayList<BufferBlockInfo> byteBufferBlocksList = new ArrayList<>();

    protected void setMemCapacity(long capacity) {
      this.memCapacity = capacity;
    }

    public long getMemCapacity() {
      return this.memCapacity;
    }

    public void setFileChannel(FileChannel fc) {
      this.channel = fc;
    }

    public void setRandomAccessFile(RandomAccessFile raf) {
      this.rdmFile = raf;
    }

    public FileChannel getFileChannel() {
      return this.channel;
    }

    public RandomAccessFile getRandomAccessFile() {
      return this.rdmFile;
    }

    public ArrayList<BufferBlockInfo> getByteBufferBlocksList() {
        return byteBufferBlocksList;
    }

    public void setByteBufferBlocksList(BufferBlockInfo bufferBlockInfo) {
        byteBufferBlocksList.add(bufferBlockInfo);
    }

}
