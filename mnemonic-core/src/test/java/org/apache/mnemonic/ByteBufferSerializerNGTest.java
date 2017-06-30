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

import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

import java.nio.ByteBuffer;
import java.util.Random;
import java.io.IOException;

/**
 * test the functionalities of ByteBufferSerializer class
 * 
 */
public class ByteBufferSerializerNGTest {

  /**
   * test to convert any serializable object from/to ByteBuffer object that is
   * backed by Java heap.
   */
  @Test
  public void testToFromByteBuffer() throws IOException, ClassNotFoundException {
    Random randomGenerator = new Random();
    for (int idx = 0; idx < 100; idx++) {
      Payload pl = new Payload(randomGenerator.nextInt(1024 * 1024),
          String.format("Str is %d", randomGenerator.nextInt(1024 * 1024)), randomGenerator.nextDouble());
      ByteBuffer bb = ByteBufferSerializer.toByteBuffer(pl);
      Payload rpl = ByteBufferSerializer.toObject(bb);
      assertTrue(pl.compareTo(rpl) == 0);
    }
  }

  /**
   * test to convert any serializable object from/to MemBufferHolder object that
   * is backed by native memory pool.
   */
  @Test
  public void testToFromMemBufferHolder() throws IOException, ClassNotFoundException {
    VolatileMemAllocator act = new VolatileMemAllocator(Utils.getVolatileMemoryAllocatorService("vmem"),
        1024 * 1024 * 1024, ".");

    Random randomGenerator = new Random();
    for (int idx = 0; idx < 100; idx++) {
      Payload pl = new Payload(randomGenerator.nextInt(1024 * 1024),
          String.format("Str is %d", randomGenerator.nextInt(1024 * 1024)), randomGenerator.nextDouble());
      MemBufferHolder<VolatileMemAllocator> mbh = ByteBufferSerializer.toMemBufferHolder(act, pl);
      Payload rpl = ByteBufferSerializer.fromMemBufferHolder(mbh);
      mbh.destroy();
      assertTrue(pl.compareTo(rpl) == 0);
    }

  }
}
