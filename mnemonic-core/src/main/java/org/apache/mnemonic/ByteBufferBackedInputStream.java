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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * an input Stream that is backed by a in-memory ByteBuffer.
 * 
 *
 */
public class ByteBufferBackedInputStream extends InputStream {

  private ByteBuffer buf;

  /**
   * accept a ByteBuffer as backed object for inputStream.
   * 
   * @param buf
   *          specify a bytebuffer that is where any data is from
   */
  public ByteBufferBackedInputStream(ByteBuffer buf) {
    this.buf = buf;
  }

  /**
   * read an integer value from backed ByteBuffer.
   * 
   * @return a integer value from stream input
   */
  public int read() throws IOException {
    if (!buf.hasRemaining()) {
      return -1;
    }
    return buf.get() & 0xFF;
  }

  /**
   * read a specified range of byte array from backed ByteBuffer.
   * 
   * @param bytes
   *          specify a output byte array to store data
   * 
   * @param off
   *          specify the offset from ByteBuffer to read
   * 
   * @param len
   *          specify the length of bytes to read
   * 
   * @return the number of bytes has been read
   */
  public int read(byte[] bytes, int off, int len) throws IOException {
    if (!buf.hasRemaining()) {
      return -1;
    }

    len = Math.min(len, buf.remaining());
    buf.get(bytes, off, len);
    return len;
  }
}
