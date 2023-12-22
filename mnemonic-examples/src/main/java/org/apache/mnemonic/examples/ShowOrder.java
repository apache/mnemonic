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

 import org.apache.mnemonic.DurableType;
 import org.apache.mnemonic.EntityFactoryProxy;
 import org.apache.mnemonic.EntityFactoryProxyHelper;
 import org.apache.mnemonic.NonVolatileMemAllocator;
 import org.apache.mnemonic.Utils;
 
 public class ShowOrder {
 
   public static void main(String[] argv) throws Exception {
     // Create a non-volatile memory pool from a memory service
     NonVolatileMemAllocator allocator = new NonVolatileMemAllocator(
         Utils.getNonVolatileMemoryAllocatorService("pmalloc"),
         1024L * 1024 * 1024, "./example_order.dat", false);
 
     // Define durable types and entity factory proxies
     DurableType[] listGenericFieldTypes = {DurableType.DURABLE};
     EntityFactoryProxy[] listEntityFactoryProxies = {new EntityFactoryProxyHelper<>(Product.class)};
 
     long handler = 0L;
 
     // Iterate over a range of key IDs (assuming key IDs are used to identify orders)
     for (long keyId = 1; keyId <= 3; keyId++) {
       // Get the handler for the specified key ID
       handler = allocator.getHandler(keyId);
 
       // If the handler is valid, restore the Order and show its details
       if (handler != 0L) {
         Order order = OrderFactory.restore(allocator, listEntityFactoryProxies, listGenericFieldTypes, handler, false);
         order.show();
       }
     }
   }
 }
 

