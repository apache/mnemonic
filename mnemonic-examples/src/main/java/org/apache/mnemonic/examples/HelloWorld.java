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

package org.apache.mnemonic.examples;

import org.apache.mnemonic.DurableBuffer;
import org.apache.mnemonic.NonVolatileMemAllocator;
import org.apache.mnemonic.Utils;
import org.apache.mnemonic.collections.DurableString;
import org.apache.mnemonic.collections.DurableStringFactory;

public class HelloWorld {

  public static void main(String[] argv) throws Exception {

    /* create a non-volatile memory pool from one of memory services */
    NonVolatileMemAllocator act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService("pmalloc"),
        1024L * 1024 * 1024, "./example_helloworld.dat", true);

    /* create durable string object */
    DurableString s = DurableStringFactory.create(act);

    /* assign a string to this durable string object */
    s.setStr("Hello World!", true);

    /* print out the string from this durable string object */
    System.out.println(Utils.ANSI_GREEN + s.getStr() + Utils.ANSI_RESET);

    /* print out the address of string */
    System.out.println(String.format("The address of this durable string object: 0x%016X",
        Utils.addressOf(Utils.getUnsafe(), s.getStr())));

    /* print out the hashcode of string */
    System.out.println(String.format("The hash code of this durable string object: 0x%08X",
        s.getStr().hashCode()));

    /* create a durable buffer, it is able to store up to 10 characters */
    DurableBuffer<?> dbuf = act.createBuffer(20);

    /* store a string into this durable buffer*/
    dbuf.get().asCharBuffer().put("hello");

    /* return a string containing the character sequence in this durable buffer */
    String dbufstr = dbuf.get().asCharBuffer().toString();

    /* print out this string */
    System.out.println(String.format("A string returned from the durable buffer: %s", dbufstr));

    /* store another string into the same durable buffer */
    dbuf.get().asCharBuffer().put("world");

    /* print out the original string */
    System.out.println(String.format("The original string with the content of this durable buffer changed: %s",
        dbufstr));

    /* return a new string containing the character sequence in this durable buffer */
    String dbufstr2 = dbuf.get().asCharBuffer().toString();

    /* print out the new string */
    System.out.println(String.format("A new string returned from the same durable buffer: %s",
        dbufstr2));

    /* print out the address of the content of this durable buffer */
    System.out.println(String.format("The address of the content of this durable buffer: 0x%016X",
        Utils.getAddressFromDirectByteBuffer(dbuf.get())));

    /* print out the base address of memory space */
    System.out.println(String.format("The base address of this memory space: 0x%016X",
        act.getEffectiveAddress(0L)));

  }
}
