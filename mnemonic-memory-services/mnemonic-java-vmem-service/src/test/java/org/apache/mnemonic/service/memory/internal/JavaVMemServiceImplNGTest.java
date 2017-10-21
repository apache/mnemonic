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

package org.apache.mnemonic.service.memory.internal;


import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;

import static org.testng.Assert.assertTrue;

/**
 * test the functionalities of JavaVMemServiceImpl class
 * 
 */
public class JavaVMemServiceImplNGTest {
  //it is created after test case - testToInitByCreateNewFile1()
  private String testFile = "./jvmstestfactory.dat";

  protected static final int MAX_BUFFER_BLOCK_SIZE = Integer.MAX_VALUE;
  @BeforeTest
  public void setUp() throws IOException {
    File file = new File("./");
    for (File f : file.listFiles()) {
      if (f.getName().startsWith("jvmstest")) {
        f.delete();
      }
    }
  }

  /**
   * test to verify the initial of memory service successes when isnew = true
   * regardless of whether the file exists or not (file doesn't exist)
   */
  @Test(enabled = true)
  public void testToInitByCreateNewFile1() throws IOException, ClassNotFoundException {
    String dest = "./jvmstestFile1.dat";
    JavaVMemServiceImpl vms = new JavaVMemServiceImpl();
    long cap = 10 * 1024 * 1024;
    long idx = vms.init(cap, dest, true);
    assertTrue(idx != -1);
    assertTrue(idx == 0);
    Assert.assertEquals(vms.getMemPools().get((int)idx).getMemCapacity(), cap);
    vms.close(idx);

    //Copy the file to generate the test data file for other test cases.
    Files.copy(Paths.get(dest), Paths.get(testFile));
    //Delete the new created test file
    Files.delete(Paths.get(dest));
  }

  /**
   * test to verify the initial of memory service successes when isnew = true
   * regardless of whether the file exists or not (file exists)
   */
  @Test(enabled = true, dependsOnMethods = "testToInitByCreateNewFile1")
  public void testToInitByCreateNewFile2() throws IOException, ClassNotFoundException {
    //Ensure it doesn't impact the test file
    String dest = "./jvmstestFile2.dat";
    Files.copy(Paths.get(testFile), Paths.get(dest));

    JavaVMemServiceImpl vms = new JavaVMemServiceImpl();
    long cap = 10 * 1024 * 1024;
    long idx = vms.init(cap, dest, true);
    assertTrue(idx != -1);
    assertTrue(idx == 0);
    Assert.assertEquals(vms.getMemPools().get((int)idx).getMemCapacity(), cap);
    vms.close(idx);

    //Delete the new created test file
    Files.delete(Paths.get(dest));

  }


  /**
   * test to verify the initial of memory service fails when isnew = true
   * and the specifiled uri is not a file
   */
  @Test(enabled = true, dependsOnMethods = "testToInitByCreateNewFile1")
  public void testToInitFailWhenNofile() throws IOException, ClassNotFoundException {
    long idx = -1;
    boolean thrown = false;
    JavaVMemServiceImpl vms = new JavaVMemServiceImpl();
    try {
      idx = vms.init(10 * 1024 * 1024, ".", true);
    } catch (Exception e) {
      thrown = true;
    } finally {
      assertTrue(thrown);
      if (idx >= 0) {
        vms.close(idx);
      }
    }
  }

  /**
   * test to verify the initial of memory service successes when isnew = false
   * and the specifiled file exists.
   */
  @Test(enabled = true, dependsOnMethods = "testToInitByCreateNewFile1")
  public void testToInitWhenFileExists() throws IOException, ClassNotFoundException {
    JavaVMemServiceImpl vms = new JavaVMemServiceImpl();
    File file = new File(testFile);
    long cap = file.length();
    long idx = vms.init(10 * 1024 * 1024, testFile, false);
    assertTrue(idx != -1);
    assertTrue(idx == 0);
    Assert.assertEquals(vms.getMemPools().get((int)idx).getMemCapacity(), cap);
    vms.close(idx);
  }

  /**
   * test to verify the initial of memory service fails when isnew = false
   * and the specifiled file doesn't exist.
   */
  @Test(enabled = true, dependsOnMethods = "testToInitByCreateNewFile1")
  public void testToInitFailWhenFileNotExists() throws IOException, ClassNotFoundException {
    long idx = -1;
    boolean thrown = false;
    JavaVMemServiceImpl vms = new JavaVMemServiceImpl();
    try {
      idx = vms.init(10 * 1024 * 1024, "./jvmstestnotexist.dat", false);
    } catch (Exception e) {
      thrown = true;
    } finally {
      assertTrue(thrown);
      if (idx >= 0) {
        vms.close(idx);
      }
    }
  }

  /**
   * test to verify the initial of memory service fails when isnew = false
   * and the specifiled uri is not a file.
   */
  @Test(enabled = true, dependsOnMethods = "testToInitByCreateNewFile1")
  public void testToInitFailWhenNotAFile() throws IOException, ClassNotFoundException {
    long idx = -1;
    boolean thrown = false;
    JavaVMemServiceImpl vms = new JavaVMemServiceImpl();
    try {
      idx = vms.init(10 * 1024 * 1024, ".", false);
    } catch (Exception e) {
      thrown = true;
    } finally {
      assertTrue(thrown);
      if (idx >= 0) {
        vms.close(idx);
      }
    }
  }

  /**
   * test to verify the memory service is initiated when capacity exceeds MAX_BUFFER_BLOCK_SIZE
   */
  @Test(enabled = true, dependsOnMethods = "testToInitByCreateNewFile1")
  public void testToInitWhenBigCapacity() throws IOException, ClassNotFoundException {
    //Ensure it doesn't impact the test file
    String dest = "./jvmstestbigcapacity.dat";
    Files.copy(Paths.get(testFile), Paths.get(dest));

    JavaVMemServiceImpl vms = new JavaVMemServiceImpl();
    long cap = 513L + 2L * 1024 * 1024 * 1024 - 1L;
    long idx = vms.init(cap, dest, true);
    MemoryInfo mi = vms.getMemPools().get((int)idx);
    assertTrue(idx == 0);
    Assert.assertEquals(mi.getMemCapacity(), cap);
    Assert.assertEquals(mi.getByteBufferBlocksList().size(), 2);
    assertTrue(mi.getByteBufferBlocksList().get(0).getBufferBlockBaseAddress() > 0);
    assertTrue(mi.getByteBufferBlocksList().get(1).getBufferBlockBaseAddress() > 0);
    Assert.assertEquals(mi.getByteBufferBlocksList().get(0).getBufferBlockSize(), MAX_BUFFER_BLOCK_SIZE);
    Assert.assertEquals(mi.getByteBufferBlocksList().get(1).getBufferBlockSize(), 513);

    vms.close(idx);
    Files.delete(Paths.get(dest));
  }

  /**
   * test to verify the byte buffer of 512Byte (one block) is created
   */
  @Test(enabled = true, dependsOnMethods = "testToInitByCreateNewFile1")
  public void testToCreateByteBufferForOneBlk1() throws IOException, ClassNotFoundException {
    //Ensure it doesn't impact the test file
    String dest = "./jvmstestcreatebufferoneblk1.dat";
    Files.copy(Paths.get(testFile), Paths.get(dest));

    JavaVMemServiceImpl vms = new JavaVMemServiceImpl();
    long cap = 10 * 1024 * 1024;
    long memPool = vms.init(cap, dest, true);
    BufferBlockInfo blockInfo = vms.getMemPools().get((int)memPool).getByteBufferBlocksList().get(0);
    ByteBuffer bytebuffer = vms.createByteBuffer(memPool, 512);
    Assert.assertNotNull(bytebuffer);
    assertTrue(blockInfo.getBufferBlockBaseAddress() > 0);
    assertTrue(blockInfo.getBufferBlockChunksMap().size() == 10 * 1024 * 1024 / 512);
    assertTrue(blockInfo.getBufferBlockChunksMap().get(0) == true);
    assertTrue(blockInfo.getBufferBlockChunksMap().get(1) == false);
    vms.destroyByteBuffer(memPool, bytebuffer, null);
    vms.close(memPool);
    Files.delete(Paths.get(dest));
  }

  /**
   * test to verify the byte buffer of more than 512Byte (two block) is created
   */
  @Test(enabled = true, dependsOnMethods = "testToInitByCreateNewFile1")
  public void testToCreateByteBufferForOneBlk2() throws IOException, ClassNotFoundException {
    //Ensure it doesn't impact the test file
    String dest = "./jvmstestcreatebufferoneblk2.dat";
    Files.copy(Paths.get(testFile), Paths.get(dest));

    JavaVMemServiceImpl vms = new JavaVMemServiceImpl();
    long cap = 10 * 1024 * 1024;
    long memPool = vms.init(cap, dest, true);
    BufferBlockInfo blockInfo = vms.getMemPools().get((int)memPool).getByteBufferBlocksList().get(0);
    ByteBuffer bytebuffer = vms.createByteBuffer(memPool, 511);
    Assert.assertNotNull(bytebuffer);
    assertTrue(blockInfo.getBufferBlockBaseAddress() > 0);
    assertTrue(blockInfo.getBufferBlockChunksMap().size() == 10 * 1024 * 1024 / 512);
    assertTrue(blockInfo.getBufferBlockChunksMap().get(0) == true);
    assertTrue(blockInfo.getBufferBlockChunksMap().get(1) == false);
    vms.destroyByteBuffer(memPool, bytebuffer, null);
    vms.close(memPool);
    Files.delete(Paths.get(dest));
  }

  /**
   * test to verify the byte buffer of more blocks is created
   */
  @Test(enabled = true, dependsOnMethods = "testToInitByCreateNewFile1")
  public void testToCreateByteBufferForMoreBlks() throws IOException, ClassNotFoundException {
    //Ensure it doesn't impact the test file
    String dest = "./jvmstestcreatebuffermoreblks.dat";
    Files.copy(Paths.get(testFile), Paths.get(dest));

    JavaVMemServiceImpl vms = new JavaVMemServiceImpl();
    long cap = 10 * 1024 * 1024;
    long memPool = vms.init(cap, dest, true);
    BufferBlockInfo blockInfo = vms.getMemPools().get((int)memPool).getByteBufferBlocksList().get(0);
    ByteBuffer bytebuffer = vms.createByteBuffer(memPool, 1025);
    Assert.assertNotNull(bytebuffer);
    assertTrue(blockInfo.getBufferBlockBaseAddress() > 0);
    assertTrue(blockInfo.getBufferBlockChunksMap().size() == 10 * 1024 * 1024 / 512);
    assertTrue(blockInfo.getBufferBlockChunksMap().get(0) == true);
    assertTrue(blockInfo.getBufferBlockChunksMap().get(1) == true);
    assertTrue(blockInfo.getBufferBlockChunksMap().get(2) == true);
    assertTrue(blockInfo.getBufferBlockChunksMap().get(3) == false);
    vms.destroyByteBuffer(memPool, bytebuffer, null);
    vms.close(memPool);
    Files.delete(Paths.get(dest));
  }

  /**
   * test to verify the byte buffer of full blocks is created
   */
  @Test(enabled = true, dependsOnMethods = "testToInitByCreateNewFile1")
  public void testToCreateByteBufferForFullBlks() throws IOException, ClassNotFoundException {
    //Ensure it doesn't impact the test file
    String dest = "./jvmstestcreatebufferfullblks.dat";
    Files.copy(Paths.get(testFile), Paths.get(dest));

    JavaVMemServiceImpl vms = new JavaVMemServiceImpl();
    long cap = 10 * 1024 * 1024;
    long memPool = vms.init(cap, dest, true);
    BufferBlockInfo blockInfo = vms.getMemPools().get((int)memPool).getByteBufferBlocksList().get(0);
    ByteBuffer bytebuffer = vms.createByteBuffer(memPool, 10 * 1024 * 1024 - 1);
    Assert.assertNotNull(bytebuffer);
    assertTrue(blockInfo.getBufferBlockBaseAddress() > 0);
    assertTrue(blockInfo.getBufferBlockChunksMap().size() == 10 * 1024 * 1024 / 512);
    assertTrue(blockInfo.getBufferBlockChunksMap().get(0) == true);
    assertTrue(blockInfo.getBufferBlockChunksMap().get(10 * 1024 * 1024 / 512 - 1) == true);
    vms.destroyByteBuffer(memPool, bytebuffer, null);
    vms.close(memPool);
    Files.delete(Paths.get(dest));
  }

  /**
   * test to verify the byte buffer is created by multiple times
   */
  @Test(enabled = true, dependsOnMethods = "testToInitByCreateNewFile1")
  public void testToCreateByteBufferMultipleTimes() throws IOException, ClassNotFoundException {
    //Ensure it doesn't impact the test file
    String dest = "./jvmstestcreatebuffermultipletimes.dat";
    Files.copy(Paths.get(testFile), Paths.get(dest));

    JavaVMemServiceImpl vms = new JavaVMemServiceImpl();
    long cap = 10 * 1024 * 1024;
    long memPool = vms.init(cap, dest, true);
    BufferBlockInfo blockInfo = vms.getMemPools().get((int)memPool).getByteBufferBlocksList().get(0);
    ByteBuffer bytebuffer1 = vms.createByteBuffer(memPool, 1024);
    Assert.assertNotNull(bytebuffer1);
    assertTrue(blockInfo.getBufferBlockChunksMap().size() == 10 * 1024 * 1024 / 512);
    assertTrue(blockInfo.getBufferBlockChunksMap().get(0) == true);
    assertTrue(blockInfo.getBufferBlockChunksMap().get(1) == true);
    assertTrue(blockInfo.getBufferBlockChunksMap().get(2) == false);
    assertTrue(blockInfo.getBufferBlockChunksMap().get(10 * 1024 * 1024 / 512 - 1) == false);

    ByteBuffer bytebuffer2 = vms.createByteBuffer(memPool, 10 * 1024 * 1024 - 1024);
    Assert.assertNotNull(bytebuffer2);
    assertTrue(blockInfo.getBufferBlockChunksMap().get(0) == true);
    assertTrue(blockInfo.getBufferBlockChunksMap().get(2) == true);
    assertTrue(blockInfo.getBufferBlockChunksMap().get(1024 * 10 * 1024 / 512 - 1) == true);
    assertTrue(blockInfo.getChunkSizeMap().size() == 2);
    ArrayList<Long> keys = new ArrayList<>();
    for (long addr : blockInfo.getChunkSizeMap().keySet()) {
      keys.add(addr);
    }
    assertTrue(keys.get(0) != keys.get(1));

    vms.destroyByteBuffer(memPool, bytebuffer1, null);
    vms.destroyByteBuffer(memPool, bytebuffer2, null);
    vms.close(memPool);
    Files.delete(Paths.get(dest));
  }

  /**
   * test to verify the byte buffer is not created when exceeding the capacity
   */
  @Test(enabled = true, dependsOnMethods = "testToInitByCreateNewFile1")
  public void testToCreateByteBufferExceedCapacity() throws IOException, ClassNotFoundException {
    //Ensure it doesn't impact the test file
    String dest = "./jvmstestcreatebufferexceedcapacity.dat";
    Files.copy(Paths.get(testFile), Paths.get(dest));

    JavaVMemServiceImpl vms = new JavaVMemServiceImpl();
    long cap = 10 * 1024 * 1024;
    long memPool = vms.init(cap, dest, true);
    BufferBlockInfo blockInfo = vms.getMemPools().get((int)memPool).getByteBufferBlocksList().get(0);
    BitSet blocksMap = blockInfo.getBufferBlockChunksMap();

    ByteBuffer bytebuffer1 = vms.createByteBuffer(memPool, 1024);
    Assert.assertNotNull(bytebuffer1);
    assertTrue(blockInfo.getBufferBlockBaseAddress() > 0);
    assertTrue(blocksMap.size() == 10 * 1024 * 1024 / 512);
    assertTrue(blocksMap.get(0) == true);
    assertTrue(blocksMap.get(1) == true);
    assertTrue(blocksMap.get(2) == false);
    assertTrue(blocksMap.get(10 * 1024 * 1024 / 512 - 1) == false);

    ByteBuffer bytebuffer2 = vms.createByteBuffer(memPool, 10 * 1024 * 1024);
    assertTrue(bytebuffer2 == null);
    assertTrue(blocksMap.get(0) == true);
    assertTrue(blocksMap.get(1) == true);
    assertTrue(blocksMap.get(2) == false);
    assertTrue(blocksMap.get(10 * 1024 * 1024 / 512 - 1) == false);
    assertTrue(blockInfo.getChunkSizeMap().size() == 1);

    vms.destroyByteBuffer(memPool, bytebuffer1, null);
    vms.close(memPool);
    Files.delete(Paths.get(dest));
  }


  /**
   * test to verify the byte buffer is destroyed successfully
   */
  @Test(enabled = true, dependsOnMethods = "testToInitByCreateNewFile1")
  public void testToDestroyByteBuffer() throws IOException, ClassNotFoundException {
    //Ensure it doesn't impact the test file
    String dest = "./jvmstestcreatebufferfordestroy.dat";
    Files.copy(Paths.get(testFile), Paths.get(dest));

    JavaVMemServiceImpl vms = new JavaVMemServiceImpl();
    long cap = 10 * 1024 * 1024;
    long memPool = vms.init(cap, dest, true);
    BufferBlockInfo blockInfo = vms.getMemPools().get((int)memPool).getByteBufferBlocksList().get(0);
    ByteBuffer bytebuffer1 = vms.createByteBuffer(memPool, 513);
    ByteBuffer bytebuffer2 = vms.createByteBuffer(memPool, 10 * 1024 * 1024 - 1024);

    // Destroy bytebuffer2 first, then destroy bytebuffer1
    vms.destroyByteBuffer(memPool, bytebuffer2, null);
    assertTrue(blockInfo.getChunkSizeMap().size() == 1);
    assertTrue(blockInfo.getBufferBlockChunksMap().get(2) == false);
    assertTrue(blockInfo.getBufferBlockChunksMap().get(10 * 1024 * 1024 / 512 - 1) == false);

    vms.destroyByteBuffer(memPool, bytebuffer1, null);
    assertTrue(blockInfo.getChunkSizeMap().size() == 0);
    assertTrue(blockInfo.getBufferBlockChunksMap().get(0) == false);
    assertTrue(blockInfo.getBufferBlockChunksMap().get(1) == false);
    vms.close(memPool);
    Files.delete(Paths.get(dest));
  }

  /**
   * test to verify the byte buffer is resized successfully.
   */
  @Test(enabled = true, dependsOnMethods = "testToInitByCreateNewFile1")
  public void testToResizeByteBuffer() throws IOException, ClassNotFoundException {
    //Ensure it doesn't impact the test file
    String dest = "./jvmstestresize.dat";
    Files.copy(Paths.get(testFile), Paths.get(dest));

    JavaVMemServiceImpl vms = new JavaVMemServiceImpl();
    long cap = 100;
    long memPool = vms.init(cap, dest, true);
    BufferBlockInfo blockInfo = vms.getMemPools().get((int)memPool).getByteBufferBlocksList().get(0);

    ByteBuffer bytebuffer1 = vms.createByteBuffer(memPool, cap);
    for (int i = 0; i < 100; i++) {
      bytebuffer1.put((byte)i);
    }

    ByteBuffer bytebuffer2 = vms.resizeByteBuffer(memPool, bytebuffer1, cap * 2);
    assertTrue(bytebuffer2.limit() == 200);
    for (int i = 0; i < cap; i++) {
      Assert.assertEquals(bytebuffer2.get(i), (byte)i);
    }

    vms.destroyByteBuffer(memPool, bytebuffer2, null);
    vms.close(memPool);
    Files.delete(Paths.get(dest));
  }

  /**
   * test to verify the retrieveByteBuffer() is working.
   */
  @Test(enabled = true, dependsOnMethods = "testToInitByCreateNewFile1")
  public void testToRetrieveByteBuffer() throws IOException, ClassNotFoundException {
    //Ensure it doesn't impact the test file
    String dest = "./jvmstestretrievebb.dat";
    Files.copy(Paths.get(testFile), Paths.get(dest));

    JavaVMemServiceImpl vms = new JavaVMemServiceImpl();
    long cap = 10 * 1024 * 1024;
    long memPool = vms.init(cap, dest, true);
    BufferBlockInfo blockInfo = vms.getMemPools().get((int)memPool).getByteBufferBlocksList().get(0);
    ByteBuffer bytebuffer = vms.createByteBuffer(memPool, 512);
    Assert.assertNotNull(bytebuffer);
    assertTrue(blockInfo.getBufferBlockBaseAddress() > 0);
    ByteBuffer bb = vms.retrieveByteBuffer(memPool, blockInfo.getBufferBlockBaseAddress());
    Assert.assertEquals(bb, bytebuffer);
    vms.destroyByteBuffer(memPool, bytebuffer, null);
    vms.close(memPool);
    Files.delete(Paths.get(dest));
  }

  /**
   * test to verify the retrieveSize() is working.
   */
  @Test(enabled = true, dependsOnMethods = "testToInitByCreateNewFile1")
  public void testToRetrieveSize() throws IOException, ClassNotFoundException {
    //Ensure it doesn't impact the test file
    String dest = "./jvmstestretrievesize.dat";
    Files.copy(Paths.get(testFile), Paths.get(dest));

    JavaVMemServiceImpl vms = new JavaVMemServiceImpl();
    long cap = 10 * 1024 * 1024;
    long memPool = vms.init(cap, dest, true);
    BufferBlockInfo blockInfo = vms.getMemPools().get((int)memPool).getByteBufferBlocksList().get(0);
    ByteBuffer bytebuffer = vms.createByteBuffer(memPool, 512);
    Assert.assertNotNull(bytebuffer);
    assertTrue(blockInfo.getBufferBlockBaseAddress() > 0);
    long size = vms.retrieveSize(memPool, blockInfo.getBufferBlockBaseAddress());
    Assert.assertEquals((int)size, 512);
    vms.destroyByteBuffer(memPool, bytebuffer, null);
    vms.close(memPool);
    Files.delete(Paths.get(dest));
  }

  /**
   * test to verify the getByteBufferHandler() is working.
   */
  @Test(enabled = true, dependsOnMethods = "testToInitByCreateNewFile1")
  public void testToGetByteBufferHandler() throws IOException, ClassNotFoundException {
    //Ensure it doesn't impact the test file
    String dest = "./jvmstestbbhandler.dat";
    Files.copy(Paths.get(testFile), Paths.get(dest));

    JavaVMemServiceImpl vms = new JavaVMemServiceImpl();
    long cap = 10 * 1024 * 1024;
    long memPool = vms.init(cap, dest, true);
    BufferBlockInfo blockInfo = vms.getMemPools().get((int)memPool).getByteBufferBlocksList().get(0);
    ByteBuffer bytebuffer = vms.createByteBuffer(memPool, 512);
    Assert.assertNotNull(bytebuffer);
    assertTrue(blockInfo.getBufferBlockBaseAddress() > 0);
    long handler = vms.getByteBufferHandler(memPool, bytebuffer);
    Assert.assertEquals(handler, blockInfo.getBufferBlockBaseAddress());
    vms.destroyByteBuffer(memPool, bytebuffer, null);
    vms.close(memPool);
    Files.delete(Paths.get(dest));
  }

  /**
   * test to verify the allocate() is working when initzero = false
   */
  @Test(enabled = true, dependsOnMethods = "testToInitByCreateNewFile1")
  public void testToAllocate() throws IOException, ClassNotFoundException {
    //Ensure it doesn't impact the test file
    String dest = "./jvmstestallocate.dat";
    Files.copy(Paths.get(testFile), Paths.get(dest));

    JavaVMemServiceImpl vms = new JavaVMemServiceImpl();
    long cap = 10 * 1024 * 1024;
    long memPool = vms.init(cap, dest, true);
    BufferBlockInfo blockInfo = vms.getMemPools().get((int)memPool).getByteBufferBlocksList().get(0);
    long handler = vms.allocate(memPool, 513, false);
    assertTrue(handler > 0L);
    ByteBuffer bb = vms.retrieveByteBuffer(memPool, handler);
    Assert.assertEquals(bb.capacity(), 513);
    for (int i = 0; i < 513; i++) {
      Assert.assertEquals(bb.get(i), (byte)0); //initiate to zero regardless of whether initzero = false
    }
    vms.destroyByteBuffer(memPool, bb, null);
    vms.close(memPool);
    Files.delete(Paths.get(dest));
  }

  /**
   * test to verify the allocate() is working when initzero = true
   */
  @Test(enabled = true, dependsOnMethods = "testToInitByCreateNewFile1")
  public void testToAllocateWithInit() throws IOException, ClassNotFoundException {
    //Ensure it doesn't impact the test file
    String dest = "./jvmstestallocatewithinit.dat";
    Files.copy(Paths.get(testFile), Paths.get(dest));

    JavaVMemServiceImpl vms = new JavaVMemServiceImpl();
    long cap = 10 * 1024 * 1024;
    long memPool = vms.init(cap, dest, true);
    BufferBlockInfo blockInfo = vms.getMemPools().get((int)memPool).getByteBufferBlocksList().get(0);
    long handler = vms.allocate(memPool, 513, true);
    assertTrue(handler > 0L);
    ByteBuffer bb = vms.retrieveByteBuffer(memPool, handler);
    Assert.assertEquals(bb.capacity(), 513);
    for (int i = 0; i < bb.capacity(); i++) {
      Assert.assertEquals(bb.get(i), (byte)0);
    }
    vms.destroyByteBuffer(memPool, bb, null);
    vms.close(memPool);
    Files.delete(Paths.get(dest));
  }

  /**
   * test to verify the reallocate() is working when initzero = true
   */
  @Test(enabled = true, dependsOnMethods = "testToInitByCreateNewFile1")
  public void testToRellocateWithInit() throws IOException, ClassNotFoundException {
    //Ensure it doesn't impact the test file
    String dest = "./jvmstestreallocatewithinit.dat";
    Files.copy(Paths.get(testFile), Paths.get(dest));

    JavaVMemServiceImpl vms = new JavaVMemServiceImpl();
    long cap = 10 * 1024 * 1024;
    long memPool = vms.init(cap, dest, true);
    BufferBlockInfo blockInfo = vms.getMemPools().get((int)memPool).getByteBufferBlocksList().get(0);
    long handler = vms.allocate(memPool, 513, false);
    long newHandler = vms.reallocate(memPool, handler, 1024, true);
    assertTrue(newHandler > 0L);
    ByteBuffer bb = vms.retrieveByteBuffer(memPool, newHandler);
    assertTrue(bb.capacity() == 1024);
    for (int i = 0; i < bb.capacity(); i++) {
      Assert.assertEquals(bb.get(i), (byte)0);
    }
    vms.destroyByteBuffer(memPool, bb, null);
    vms.close(memPool);
    Files.delete(Paths.get(dest));
  }


  /**
   * test to verify the reallocate() is working when initzero = false
   */
  @Test(enabled = true, dependsOnMethods = "testToInitByCreateNewFile1")
  public void testToRellocate() throws IOException, ClassNotFoundException {
    //Ensure it doesn't impact the test file
    String dest = "./jvmstestreallocate.dat";
    Files.copy(Paths.get(testFile), Paths.get(dest));

    JavaVMemServiceImpl vms = new JavaVMemServiceImpl();
    long cap = 10 * 1024 * 1024;
    long memPool = vms.init(cap, dest, true);
    BufferBlockInfo blockInfo = vms.getMemPools().get((int)memPool).getByteBufferBlocksList().get(0);
    long handler = vms.allocate(memPool, 513, false);
    long newHandler = vms.reallocate(memPool, handler, 1024, false);
    assertTrue(newHandler > 0L);
    ByteBuffer bb = vms.retrieveByteBuffer(memPool, newHandler);
    assertTrue(bb.capacity() == 1024);
    for (int i = 0; i < bb.capacity(); i++) {
      Assert.assertEquals(bb.get(i), (byte)0);
    }
    vms.destroyByteBuffer(memPool, bb, null);
    vms.close(memPool);
    Files.delete(Paths.get(dest));
  }

}
