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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

/**
 * MemoryInfo class represents information about memory capacity and related resources.
 */
public class MemoryInfo {

    private long memCapacity; // Memory capacity in bytes
    private FileChannel channel; // FileChannel for memory access
    private RandomAccessFile rdmFile; // RandomAccessFile for memory access
    private ArrayList<BufferBlockInfo> byteBufferBlocksList; // List of buffer block information

    /**
     * Sets the memory capacity.
     *
     * @param capacity the memory capacity to set
     */
    protected void setMemCapacity(long capacity) {
        this.memCapacity = capacity;
    }

    /**
     * Gets the memory capacity.
     *
     * @return the memory capacity
     */
    public long getMemCapacity() {
        return this.memCapacity;
    }

    /**
     * Sets the FileChannel for memory access.
     *
     * @param fc the FileChannel to set
     */
    public void setFileChannel(FileChannel fc) {
        this.channel = fc;
    }

    /**
     * Sets the RandomAccessFile for memory access.
     *
     * @param raf the RandomAccessFile to set
     */
    public void setRandomAccessFile(RandomAccessFile raf) {
        this.rdmFile = raf;
    }

    /**
     * Gets the FileChannel for memory access.
     *
     * @return the FileChannel
     */
    public FileChannel getFileChannel() {
        return this.channel;
    }

    /**
     * Gets the RandomAccessFile for memory access.
     *
     * @return the RandomAccessFile
     */
    public RandomAccessFile getRandomAccessFile() {
        return this.rdmFile;
    }

    /**
     * Gets the list of buffer block information.
     *
     * @return the list of buffer block information
     */
    public ArrayList<BufferBlockInfo> getByteBufferBlocksList() {
        if (byteBufferBlocksList == null) {
            byteBufferBlocksList = new ArrayList<>();
        }
        return byteBufferBlocksList;
    }

    /**
     * Adds buffer block information to the list.
     *
     * @param bufferBlockInfo the buffer block information to add
     */
    public void setByteBufferBlocksList(BufferBlockInfo bufferBlockInfo) {
        getByteBufferBlocksList().add(bufferBlockInfo);
    }

    /**
     * Closes the resources associated with the MemoryInfo object.
     * It closes the FileChannel and RandomAccessFile if they are not null.
     */
    public void close() {
        try {
            if (channel != null) {
                channel.close();
            }
            if (rdmFile != null) {
                rdmFile.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
