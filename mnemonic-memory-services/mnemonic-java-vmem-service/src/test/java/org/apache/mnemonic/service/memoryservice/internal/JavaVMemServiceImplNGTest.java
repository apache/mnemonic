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

package org.apache.mnemonic.service.memoryservice.internal;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.testng.annotations.Test;
import static org.testng.Assert.assertTrue;

/**
 * test the functionalities of JavaVMemServiceImpl class
 * 
 */
public class JavaVMemServiceImplNGTest {
   private String testFile = "./jvmstest.dat";
  /**
   * test to verify the initial of memory service successes when isnew = true
   * regardless of whether the file exists or not (file exists)
   */
  @Test
  public void testToInitByCreateNewFile1() throws IOException, ClassNotFoundException {
    //Ensure it doesn't impact the test file
    String dest = "./jvmstestCopy.dat";
    Files.copy(Paths.get(testFile), Paths.get(dest));

    JavaVMemServiceImpl jvms = new JavaVMemServiceImpl();
    long cap = 10 * 1024 * 1024;
    long idx = jvms.init(10 * 1024 * 1024, dest, true);
    assertTrue(idx != -1);
    assertTrue(idx == 0);
    assertTrue(jvms.getMInfo().get(idx).equals(cap));
    jvms.close(idx);
    Files.delete(Paths.get(dest));
  }

  /**
   * test to verify the initial of memory service successes when isnew = true
   * regardless of whether the file exists or not (file doesn't exist)
   */
  @Test
  public void testToInitByCreateNewFile2() throws IOException, ClassNotFoundException {
    JavaVMemServiceImpl jvms = new JavaVMemServiceImpl();
    long cap = 10 * 1024 * 1024;
    long idx = jvms.init(10 * 1024 * 1024, "./jvmstestnotexist.dat", true);
    assertTrue(idx != -1);
    assertTrue(idx == 0);
    assertTrue(jvms.getMInfo().get(idx).equals(cap));

    //Delete the new created test file
    File file = new File("./jvmstestnotexist.dat");
    file.delete();
    jvms.close(idx);
  }



  /**
   * test to verify the initial of memory service fails when isnew = true
   * and the specifiled uri is not a file
   */
  @Test
  public void testToInitFailWhenNofile() throws IOException, ClassNotFoundException {
    long idx = -1;
    boolean thrown = false;
    JavaVMemServiceImpl jvms = new JavaVMemServiceImpl();
    try {
      idx = jvms.init(10 * 1024 * 1024, ".", true);
    } catch (Exception e) {
      thrown = true;
    } finally {
      assertTrue(thrown);
      if (idx >= 0) {
        jvms.close(idx);
      }
    }
  }

  /**
   * test to verify the initial of memory service successes when isnew = false
   * and the specifiled file exists.
   */
  @Test
  public void testToInitWhenFileExists() throws IOException, ClassNotFoundException {
    JavaVMemServiceImpl jvms = new JavaVMemServiceImpl();
    File file = new File(testFile);
    long cap = file.length();
    long idx = jvms.init(10 * 1024 * 1024, testFile, false);
    assertTrue(idx != -1);
    assertTrue(idx == 0);
    assertTrue(jvms.getMInfo().get(idx).equals(cap));
    jvms.close(idx);
  }

  /**
   * test to verify the initial of memory service fails when isnew = false
   * and the specifiled file doesn't exist.
   */
  @Test
  public void testToInitFailWhenFileNotExists() throws IOException, ClassNotFoundException {
    long idx = -1;
    boolean thrown = false;
    JavaVMemServiceImpl jvms = new JavaVMemServiceImpl();
    try {
      idx = jvms.init(10 * 1024 * 1024, "./jvmstestnotexist.dat", false);
    } catch (Exception e) {
      thrown = true;
    } finally {
      assertTrue(thrown);
      if (idx >= 0) {
        jvms.close(idx);
      }
    }
  }

  /**
   * test to verify the initial of memory service fails when isnew = false
   * and the specifiled uri is not a file.
   */
  @Test
  public void testToInitFailWhenNotAFile() throws IOException, ClassNotFoundException {
    long idx = -1;
    boolean thrown = false;
    JavaVMemServiceImpl jvms = new JavaVMemServiceImpl();
    try {
      idx = jvms.init(10 * 1024 * 1024, ".", false);
    } catch (Exception e) {
      thrown = true;
    } finally {
      assertTrue(thrown);
      if (idx >= 0) {
        jvms.close(idx);
      }
    }
  }
}
