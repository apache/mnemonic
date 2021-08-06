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

package org.apache.mnemonic.collections;

//import java.util.Iterator;
import java.nio.ByteBuffer;
import java.util.Random;

import org.apache.mnemonic.EntityFactoryProxyHelper;
import org.apache.mnemonic.Utils;
import org.apache.mnemonic.NonVolatileMemAllocator;
//import org.apache.mnemonic.OutOfHybridMemory;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.Reclaim;
//import org.apache.mnemonic.Durable;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.AssertJUnit;
//import org.testng.Assert;

/**
 *
 *
 */

public class DurableTreeNGTest {
  private long cKEYCAPACITY;
  private NonVolatileMemAllocator m_act;
  private Random rand;
  @SuppressWarnings({"restriction", "UseOfSunClasses"})
  private sun.misc.Unsafe unsafe;

  @BeforeClass
  public void setUp() throws Exception {
    rand = Utils.createRandom();
    unsafe = Utils.getUnsafe();
    m_act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService("pmalloc"), 1024 * 1024 * 1024,
        "./pobj_tree.dat", true);
    cKEYCAPACITY = m_act.handlerCapacity();
    m_act.setBufferReclaimer(new Reclaim<ByteBuffer>() {
      @Override
      public boolean reclaim(ByteBuffer mres, Long sz) {
        //System.out.println(String.format("Reclaim Memory Buffer: %X  Size: %s", System.identityHashCode(mres),
        //    null == sz ? "NULL" : sz.toString()));
        //System.out.println(" Reclaim String buffer " + mres.asCharBuffer().toString());
        return false;
      }
    });
    m_act.setChunkReclaimer(new Reclaim<Long>() {
      @Override
      public boolean reclaim(Long mres, Long sz) {
        //System.out.println(String.format("Reclaim Memory Chunk: %X  Size: %s", System.identityHashCode(mres),
        //    null == sz ? "NULL" : sz.toString()));
        return false;
      }
    });

    for (long i = 0; i < cKEYCAPACITY; ++i) {
      m_act.setHandler(i, 0L);
    }
  }

  protected int randInt() {
    return rand.nextInt(1024 * 1024);
  }

  @AfterClass
  public void tearDown() {
    m_act.close();
  }

  @Test(enabled = true)
  public void testInsertIntegers() {
    DurableType gtypes[] = {DurableType.INTEGER};
    DurableTree<Integer> tree = DurableTreeFactory.create(m_act, null, gtypes, false);

    Long handler = tree.getHandler();
    tree.insert(5);
    tree.insert(3);
    tree.insert(4);
    tree.insert(1);
    tree.insert(7);
    tree.insert(6);
    AssertJUnit.assertTrue(tree.isValidTree());

    AssertJUnit.assertTrue(tree.contains(4));
    AssertJUnit.assertFalse(tree.contains(8));
    AssertJUnit.assertTrue(tree.contains(7));
    AssertJUnit.assertTrue(tree.contains(5));

    AssertJUnit.assertEquals(tree.successor(4).intValue(), 5);
    AssertJUnit.assertEquals(tree.predecessor(4).intValue(), 3);
    AssertJUnit.assertNull(tree.predecessor(1));
    AssertJUnit.assertEquals(tree.successor(1).intValue(), 3);
    AssertJUnit.assertEquals(tree.predecessor(7).intValue(), 6);
    AssertJUnit.assertNull(tree.successor(7));

    AssertJUnit.assertTrue(tree.contains(3));
    tree.remove(3, true);
    AssertJUnit.assertFalse(tree.contains(3));
    AssertJUnit.assertEquals(tree.successor(1).intValue(), 4);
    AssertJUnit.assertEquals(tree.predecessor(4).intValue(), 1);
    AssertJUnit.assertTrue(tree.isValidTree());
    tree.remove(7, true);
    AssertJUnit.assertFalse(tree.contains(7));
    AssertJUnit.assertNull(tree.successor(6));
    AssertJUnit.assertTrue(tree.isValidTree());

    tree.insert(3);
    tree.insert(7);

    DurableTree<Integer> restoredTree = DurableTreeFactory.restore(m_act, null, gtypes, handler, false);

    AssertJUnit.assertTrue(restoredTree.isValidTree());

    AssertJUnit.assertTrue(restoredTree.contains(4));
    AssertJUnit.assertFalse(restoredTree.contains(8));
    AssertJUnit.assertTrue(restoredTree.contains(7));
    AssertJUnit.assertTrue(restoredTree.contains(5));

    AssertJUnit.assertEquals(restoredTree.successor(4).intValue(), 5);
    AssertJUnit.assertEquals(restoredTree.predecessor(4).intValue(), 3);
    AssertJUnit.assertNull(restoredTree.predecessor(1));
    AssertJUnit.assertEquals(restoredTree.successor(1).intValue(), 3);
    AssertJUnit.assertEquals(restoredTree.predecessor(7).intValue(), 6);
    AssertJUnit.assertNull(restoredTree.successor(7));

    restoredTree.insert(10);
    AssertJUnit.assertEquals(restoredTree.successor(1).intValue(), 3);
    AssertJUnit.assertEquals(restoredTree.predecessor(10).intValue(), 7);

    restoredTree.destroy();
  }

  @Test(enabled = true)
  public void testInsertString() {
    DurableType gtypes[] = {DurableType.STRING};
    DurableTree<String> tree = DurableTreeFactory.create(m_act, null, gtypes, false);

    //strings are sorted lexicographically
    Long handler = tree.getHandler();
    tree.insert("bob");
    tree.insert("Alice");
    tree.insert("Dog");
    tree.insert("Fun");
    tree.insert("Ele");
    tree.insert("Cat");

    AssertJUnit.assertTrue(tree.isValidTree());

    AssertJUnit.assertTrue(tree.contains("Fun"));
    AssertJUnit.assertFalse(tree.contains("is"));
    AssertJUnit.assertTrue(tree.contains("Cat"));
    AssertJUnit.assertFalse(tree.contains("Bob"));

    AssertJUnit.assertEquals(tree.successor("Ele"), "Fun");
    AssertJUnit.assertEquals(tree.predecessor("Dog"), "Cat");
    AssertJUnit.assertNull(tree.predecessor("Alice"));
    AssertJUnit.assertEquals(tree.successor("Alice"), "Cat");
    AssertJUnit.assertEquals(tree.predecessor("Fun"), "Ele");
    AssertJUnit.assertNull(tree.successor("bob"));

    tree.remove("Cat", true);
    AssertJUnit.assertEquals(tree.predecessor("Dog"), "Alice");
    AssertJUnit.assertEquals(tree.successor("Alice"), "Dog");
    AssertJUnit.assertTrue(tree.isValidTree());
    tree.remove("Alice", true);
    AssertJUnit.assertNull(tree.predecessor("Dog"));

    tree.destroy();
  }

  @Test(enabled = true)
  public void testInsertRandomBigTrees() {
    DurableType gtypes[] = {DurableType.INTEGER};
    DurableTree<Integer> tree = DurableTreeFactory.create(m_act, null, gtypes, false);

    Long handler = tree.getHandler();
    for (int i = 0; i < 10 * 1024; i++) {
      int rand = randInt();
      tree.insert(rand);
      AssertJUnit.assertTrue(tree.contains(rand));
    }
    AssertJUnit.assertTrue(tree.isValidTree());
    tree.destroy();
  }

  @Test(enabled = true)
  public void testInsertRemoveBigTrees() {
    DurableType gtypes[] = {DurableType.INTEGER};
    DurableTree<Integer> tree = DurableTreeFactory.create(m_act, null, gtypes, false);

    Long handler = tree.getHandler();
    for (int i = 0; i < 10 * 1024; i++) {
      tree.insert(i);
      AssertJUnit.assertTrue(tree.contains(i));
    }

    AssertJUnit.assertTrue(tree.isValidTree());

    for (int i = 0; i < 5 * 1024; i++) {
      AssertJUnit.assertTrue(tree.remove(i, true));
    }
    AssertJUnit.assertTrue(tree.isValidTree());

    for (int i = 0; i < 10 * 1024; i++) {
      if (i < 5 * 1024) {
        AssertJUnit.assertFalse(tree.contains(i));
        tree.insert(i);
      } else {
        AssertJUnit.assertTrue(tree.contains(i));
      }
    }

    for (int i = 0; i < 10 * 1024; i++) {
      AssertJUnit.assertTrue(tree.contains(i));
    }
    AssertJUnit.assertTrue(tree.isValidTree());

    tree.destroy();
  }

  @Test(enabled = true)
  public void testInsertDurable() throws NoSuchMethodException, ClassNotFoundException {
    DurableType gtypes[] = {DurableType.DURABLE};
    EntityFactoryProxy efproxies[] = {new EntityFactoryProxyHelper<Person>(Person.class)};

    DurableTree<Person<Long>> tree = DurableTreeFactory.create(m_act, efproxies, gtypes, false);
    Long handler = tree.getHandler();

    for (int i = 0; i < 10; i++) {
      Person<Long> person =  (Person<Long>) efproxies[0].create(m_act, null, null, false);
      person.setAge((short) (40 - i));
      tree.insert(person);
    }

    AssertJUnit.assertTrue(tree.isValidTree());
    tree.destroy();
  }
}
