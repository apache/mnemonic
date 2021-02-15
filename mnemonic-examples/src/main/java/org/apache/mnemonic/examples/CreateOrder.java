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
import org.apache.mnemonic.collections.DurableSinglyLinkedList;
import org.apache.mnemonic.collections.DurableSinglyLinkedListFactory;

import java.text.SimpleDateFormat;

public class CreateOrder {

  public static void main(String[] argv) throws Exception {

    /* create a non-volatile memory pool from one of memory services */
    NonVolatileMemAllocator act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService("pmalloc"),  
        1024L * 1024 * 1024, "./example_order.dat", true);

    System.out.printf("Creating Customer info...\n");
    Customer c1 = CustomerFactory.create(act);
    c1.setName("Customer Name 1", true);

    Customer c2 = CustomerFactory.create(act);
    c2.setName("Customer Name 2", true);

    System.out.printf("Creating Product info...\n");
    Product p1 = ProductFactory.create(act);
    p1.setName("Product Name 1", true);
    p1.setPrice(34.2, true);

    Product p2 = ProductFactory.create(act);
    p2.setName("Product Name 2", true);
    p2.setPrice(6.5, true);

    Product p3 = ProductFactory.create(act);
    p3.setName("Product Name 3", true);
    p3.setPrice(185.7, true);

    Product p4 = ProductFactory.create(act);
    p4.setName("Product Name 4", true);
    p4.setPrice(67.99, true);

    SimpleDateFormat datefmt = new SimpleDateFormat("dd-MMM-yyyy");

    DurableType listgftypes[] = {DurableType.DURABLE};

    EntityFactoryProxy listefproxies[] = {new EntityFactoryProxyHelper<Product>(Product.class) };

    System.out.printf("Creating Order info...\n");
    DurableSinglyLinkedList<Product> list1 = DurableSinglyLinkedListFactory.create(act, listefproxies,
        listgftypes, false);
    list1.add(p2);
    list1.add(p4);

    Order o1 = OrderFactory.create(act);
    o1.setId("Order #1 ID: 2115845-1669829", true);
    o1.setDate(datefmt.parse("12-December-2016"), true);
    o1.setCustomer(c2, true);
    o1.setItems(list1, true);

    DurableSinglyLinkedList<Product> list2 = DurableSinglyLinkedListFactory.create(act, listefproxies,
        listgftypes, false);
    list2.add(p3);
    list2.add(p2);

    Order o2 = OrderFactory.create(act);
    o2.setId("Order #2 ID: 4264480-5197805", true);
    o2.setDate(datefmt.parse("06-August-2017"), true);
    o2.setCustomer(c1, true);
    o2.setItems(list2, true);

    DurableSinglyLinkedList<Product> list3 = DurableSinglyLinkedListFactory.create(act, listefproxies,
        listgftypes, false);
    list3.add(p1);

    Order o3 = OrderFactory.create(act);
    o3.setId("Order #3 ID: 0183170-8910615", true);
    o3.setDate(datefmt.parse("27-July-2017"), true);
    o3.setCustomer(c2, true);
    o3.setItems(list3, true);

    o1.show();
    o2.show();
    o3.show();

    act.setHandler(1, o1.getHandler());
    act.setHandler(2, o2.getHandler());
    act.setHandler(3, o3.getHandler());

  }
}
