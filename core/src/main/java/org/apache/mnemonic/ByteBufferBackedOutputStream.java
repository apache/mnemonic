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
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * an output Stream that is backed by a in-memory ByteBuffer.
 * 
 *
 */
public class ByteBufferBackedOutputStream extends OutputStream {

  private ByteBuffer buf;

  /**
   * accept a ByteBuffer to store external data, the capacity of it could be
   * extended at will.
   * 
   * @param buf
   *          specify a ByteBuffer object that is used to store external data to
   *          its backed buffer
   * 
   */
  public ByteBufferBackedOutputStream(ByteBuffer buf) {
    this.buf = buf;
  }

  /**
   * write an integer value to backed buffer.
   * 
   * @param b
   *          specify an integer value to be written
   */
  public void write(int b) throws IOException {
    buf.put((byte) b);
  }

  /**
   * write an array of bytes to a specified range of backed buffer
   * 
   * @param bytes
   *          specify a byte array to write
   * 
   * @param off
   *          specify the offset of backed buffer where is start point to be
   *          written
   * 
   * @param len
   *          specify the length of bytes to be written
   */
  public void write(byte[] bytes, int off, int len) throws IOException {
    buf.put(bytes, off, len);
  }

}
