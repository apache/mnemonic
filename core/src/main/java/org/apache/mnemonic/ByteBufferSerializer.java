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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.io.InputStream;

/**
 * a serializer class that manages to serialize any serilizable objects from/to
 * ByteBuffer.
 * 
 */

public class ByteBufferSerializer {

  /**
   * serialize object to a ByteBuffer object.
   * 
   * @param <ValueT>
   *          the type of object
   * 
   * @param obj
   *          specify a object that is serializable
   * 
   * @throws IOException
   *           the exception of serialization
   * 
   * @return a ByteBuffer object contained serialized object
   */
  public static <ValueT> ByteBuffer toByteBuffer(ValueT obj) throws IOException {
    byte[] bytes = null;
    ByteArrayOutputStream bos = null;
    ObjectOutputStream oos = null;
    try {
      bos = new ByteArrayOutputStream();
      oos = new ObjectOutputStream(bos);
      oos.writeObject(obj);
      oos.flush();
      bytes = bos.toByteArray();
    } finally {
      if (oos != null) {
        oos.close();
      }
      if (bos != null) {
        bos.close();
      }
    }
    return ByteBuffer.wrap(bytes);
  }

  /**
   * de-serialize an object from a ByteBuffer object.
   * 
   * @param <ValueT>
   *          the type of object
   * 
   * @param bf
   *          specify an ByteBuffer contains data that can be de-serialized
   * 
   * @throws IOException
   *           the exception of deserialization
   * 
   * @throws ClassNotFoundException
   *           Not found class of de-serialized object
   * 
   * @return a de-serialized object
   */
  @SuppressWarnings("unchecked")
  public static <ValueT> ValueT toObject(ByteBuffer bf) throws IOException, ClassNotFoundException {
    Object obj = null;
    InputStream is = null;
    ObjectInputStream ois = null;
    try {
      is = new ByteBufferBackedInputStream(bf);
      ois = new ObjectInputStream(is);
      obj = ois.readObject();
    } finally {
      if (is != null) {
        is.close();
      }
      if (ois != null) {
        ois.close();
      }
    }
    return (ValueT) obj;
  }

  /**
   * serialize object to a MemBufferHolder object.
   * 
   * @param <A>
   *          the type of bound allocator
   * 
   * @param <ValueT>
   *          the type of object
   * 
   * @param ar
   *          specify an Allocator that is used to generate MemBufferHolder
   *          which is backed by a native memory block
   * 
   * @param obj
   *          specify a object that is serializable
   * 
   * @throws IOException
   *           the exception of serialization
   * 
   * @return a MemBufferHolder object contained serialized object
   */
  public static <A extends CommonAllocator<A>, ValueT> MemBufferHolder<A> toMemBufferHolder(A ar, ValueT obj)
      throws IOException {
    MemBufferHolder<A> ret = null;
    ByteBuffer bb = toByteBuffer(obj);
    if (null != bb && bb.remaining() > 0) {
      ret = ar.createBuffer(bb.remaining());
      ret.get().put(bb);
      ret.get().flip();
    }
    return ret;
  }

  /**
   * de-serialize an object from a MemBufferHolder object.
   * 
   * @param <A>
   *          the type of bound allocator
   * 
   * @param <ValueT>
   *          the type of object
   * 
   * @param mbh
   *          specify an MemBufferHolder who contains data that can be
   *          de-serialized
   * 
   * @throws IOException
   *           the exception of deserialization
   * 
   * @throws ClassNotFoundException
   *           Not found class of de-serialized object
   * 
   * @return a de-serialized object
   */
  public static <A extends CommonAllocator<A>, ValueT> ValueT fromMemBufferHolder(MemBufferHolder<A> mbh)
      throws IOException, ClassNotFoundException {
    return toObject(mbh.get());
  }
}
