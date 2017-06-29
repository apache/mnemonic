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

package org.apache.mnemonic;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This class used to present a region of DurableChunk as a buffer.
 */
public class ChunkBuffer<A extends RetrievableAllocator<A>> {

  protected DurableChunk<A> m_dchunk = null;
  protected ByteBuffer m_buffer = null;

  public ChunkBuffer(DurableChunk<A> dchunk, long offset, int size) {
    Field address, capacity;
    m_dchunk = dchunk;
    if (null != dchunk && size > 0 && offset >= 0
            && offset + size <= dchunk.getSize()) {
      ByteBuffer bb = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder());
      try {
        address = Buffer.class.getDeclaredField("address");
        address.setAccessible(true);
        capacity = Buffer.class.getDeclaredField("capacity");
        capacity.setAccessible(true);
        address.setLong(bb, dchunk.get() + offset);
        capacity.setInt(bb, size);
        m_buffer = bb;
      } catch (NoSuchFieldException e) {
        throw new ConfigurationException("Buffer fields not found.");
      } catch (IllegalAccessException e) {
        throw new ConfigurationException("Buffer fields cannot be accessed.");
      }
    } else {
      throw new OutOfBoundsException("The ChunkBuffer is out of bounds of its backed DurableChunk.");
    }
  }

  public ByteBuffer get() {
    return m_buffer;
  }

  public DurableChunk<A> getChunk() {
    return m_dchunk;
  }
}

