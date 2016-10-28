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

/**
 * This interface to randomly access the in-memory stream. 
 *
 */
public interface MemStream {

  /**
   * This function will close the stream and release any 
   * system resources associated with the stream.
   * 
   */
  void close(); 

  /**
   * This function retrieves the current offset in this in-memory stream.
   * 
   * @return the long-type offset value
   */
  long getPosition(); 

  /**
   * This function returns the length of this in-memory stream.
   * 
   * @return the long-type length
   */
  long length(); 

  /**
   * This function reads a byte of data from the stream.
   * 
   * @return the 1-byte of data, or -1 if reaching the end of stream
   */
  int read(); 

  /**
   * This function reads up to b.length bytes of data from the stream into
   * an array of bytes
   * 
   * @param b
   *          array of bytes into which data is read
   * @return the total number of bytes read into the array, or -1 if 
   *         reaching the end of streamint-type data of one byte
   */
  int read(byte[] b);


  /**
   * This function reads up to len bytes of data from the stream into
   * an array of bytes
   * 
   * @param b
   *          array of bytes into which data is read
   * @param off 
   *          the start offset in array b at which the data is written
   * @param len
   *          the maximum number of bytes read   
   * 
   * @return the total number of bytes read into the array, or -1 if 
   *         reaching the end of streamint-type data of one byte
   */
 
  int read(byte[] b, int off, int len); 

  /**
   * More read functions can be added, such as readDouble, readLine and so on
   *
   */

  /**
   * This function sets the mem-pointer offset, measured from the beginning
   * of the stream, which is the starting position for the next read
   * or write
   *
   * @param pos
   *          the offset position, measured from the beginning of the
   *          mem stream
   * @return the new offset, or -1 if the offset is longer than the length of the mem stream
   */
  long seek(long pos); 

  /**
   * This function writes b.length bytes from an array of bytes
   *
   * @param b
   *          array of bytes
   */
  void write(byte[] b);

  /**
   * This function writes len bytes from an array of bytes
   * starting from offset off
   *
   * @param b
   *          array of bytes
   * @param off
   *          the starting offset in array b at which the data is read
   * @param len
   *          the maximum number of bytes to write
   */
  void write(byte[] b, int off, int len);

  /**
   * This function write byte b into the mem stream
   *
   * @param b
   *          the data
   * 
   */
  void write(int b);

  /**
   * More write functions can be added
   *
   */

}
