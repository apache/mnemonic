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

    /* create a non-volatile memory pool from one of memory services */
    NonVolatileMemAllocator act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService("pmalloc"),
        1024L * 1024 * 1024, "./example_order.dat", false);

    DurableType listgftypes[] = {DurableType.DURABLE};

    EntityFactoryProxy listefproxies[] = {new EntityFactoryProxyHelper<Product>(Product.class) };

    long hdl = 0L;
    for (long keyid = 1; keyid <= 3; keyid++) {
      hdl = act.getHandler(keyid);
      if (0L != hdl) {
        Order o = OrderFactory.restore(act, listefproxies, listgftypes, hdl, false);
        o.show();
      }
    }
  }
}
