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

  }
}
