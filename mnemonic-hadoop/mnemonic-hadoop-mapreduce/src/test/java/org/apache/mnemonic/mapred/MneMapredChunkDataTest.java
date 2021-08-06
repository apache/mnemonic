/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mnemonic.mapred;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.OutputFormat;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.TaskAttemptID;
import org.apache.hadoop.mapred.TaskAttemptContext;
import org.apache.hadoop.mapred.TaskAttemptContextImpl;
import org.apache.hadoop.mapreduce.TaskType;
import org.apache.mnemonic.DurableChunk;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.Utils;
import org.apache.mnemonic.hadoop.MneConfigHelper;
import org.apache.mnemonic.hadoop.MneDurableInputValue;
import org.apache.mnemonic.hadoop.MneDurableInputSession;
import org.apache.mnemonic.hadoop.MneDurableOutputSession;
import org.apache.mnemonic.hadoop.MneDurableOutputValue;
import org.apache.mnemonic.hadoop.mapred.MneInputFormat;
import org.apache.mnemonic.hadoop.mapred.MneOutputFormat;
import org.apache.mnemonic.sessions.SessionIterator;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

@SuppressWarnings("restriction")
public class MneMapredChunkDataTest {

  private static final String DEFAULT_BASE_WORK_DIR = "target" + File.separator + "test" + File.separator + "tmp";
  private static final String DEFAULT_WORK_DIR = DEFAULT_BASE_WORK_DIR + File.separator + "chunk-data";
  private static final String SERVICE_NAME = "pmalloc";
  private static final long SLOT_KEY_ID = 5L;
  private Path m_workdir;
  private JobConf m_conf;
  private FileSystem m_fs;
  private Random m_rand;
  private TaskAttemptID m_taid;
  private TaskAttemptContext m_tacontext;
  private long m_reccnt = 5000L;
  private volatile long m_checksum;
  private volatile long m_totalsize = 0L;
  @SuppressWarnings({"restriction", "UseOfSunClasses"})
  private sun.misc.Unsafe unsafe;

  @BeforeClass
  public void setUp() throws Exception {
    m_workdir = new Path(
            System.getProperty("test.tmp.dir", DEFAULT_WORK_DIR));
    m_conf = new JobConf();
    m_rand = Utils.createRandom();
    unsafe = Utils.getUnsafe();

    try {
      m_fs = FileSystem.getLocal(m_conf).getRaw();
      m_fs.delete(m_workdir, true);
      m_fs.mkdirs(m_workdir);
    } catch (IOException e) {
      throw new IllegalStateException("bad fs init", e);
    }

    m_taid = new TaskAttemptID("jt", 0, TaskType.MAP, 0, 0);
    m_tacontext = new TaskAttemptContextImpl(m_conf, m_taid);

    MneConfigHelper.setDir(m_conf, MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX, m_workdir.toString());
    MneConfigHelper.setBaseOutputName(m_conf, null, "chunk-data");

    MneConfigHelper.setMemServiceName(m_conf, MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX, SERVICE_NAME);
    MneConfigHelper.setSlotKeyId(m_conf, MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX, SLOT_KEY_ID);
    MneConfigHelper.setDurableTypes(m_conf,
            MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX, new DurableType[]{DurableType.CHUNK});
    MneConfigHelper.setEntityFactoryProxies(m_conf,
            MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX, new Class<?>[]{});
    MneConfigHelper.setMemServiceName(m_conf, MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX, SERVICE_NAME);
    MneConfigHelper.setSlotKeyId(m_conf, MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX, SLOT_KEY_ID);
    MneConfigHelper.setMemPoolSize(m_conf,
            MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX, 1024L * 1024 * 1024 * 4);
    MneConfigHelper.setDurableTypes(m_conf,
            MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX, new DurableType[]{DurableType.CHUNK});
    MneConfigHelper.setEntityFactoryProxies(m_conf,
            MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX, new Class<?>[]{});
  }

  @AfterClass
  public void tearDown() {

  }

  protected DurableChunk<?> genupdDurableChunk(
          MneDurableOutputSession<DurableChunk<?>> s, Checksum cs) {
    DurableChunk<?> ret = null;
    int sz = m_rand.nextInt(1024 * 1024) + 1024 * 1024;
    ret = s.newDurableObjectRecord(sz);
    byte b;
    if (null != ret) {
      for (int i = 0; i < ret.getSize(); ++i) {
        b = (byte) m_rand.nextInt(255);
        unsafe.putByte(ret.get() + i, b);
        cs.update(b);
      }
      m_totalsize += sz;
    }
    return ret;
  }

  @Test(enabled = true)
  public void testWriteChunkData() throws Exception {
    NullWritable nada = NullWritable.get();
    MneDurableOutputSession<DurableChunk<?>> sess =
            new MneDurableOutputSession<DurableChunk<?>>(m_tacontext, null,
                    MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX);
    MneDurableOutputValue<DurableChunk<?>> mdvalue =
            new MneDurableOutputValue<DurableChunk<?>>(sess);
    OutputFormat<NullWritable, MneDurableOutputValue<DurableChunk<?>>> outputFormat =
            new MneOutputFormat<MneDurableOutputValue<DurableChunk<?>>>();
    RecordWriter<NullWritable, MneDurableOutputValue<DurableChunk<?>>> writer =
            outputFormat.getRecordWriter(null, m_conf, null, null);
    DurableChunk<?> dchunk = null;
    Checksum cs = new CRC32();
    cs.reset();
    for (int i = 0; i < m_reccnt; ++i) {
      dchunk = genupdDurableChunk(sess, cs);
      Assert.assertNotNull(dchunk);
      writer.write(nada, mdvalue.of(dchunk));
    }
    m_checksum = cs.getValue();
    writer.close(null);
    sess.close();
  }

  @Test(enabled = true, dependsOnMethods = {"testWriteChunkData"})
  public void testReadChunkData() throws Exception {
    List<String> partfns = new ArrayList<String>();
    long reccnt = 0L;
    long tsize = 0L;
    Checksum cs = new CRC32();
    cs.reset();
    File folder = new File(m_workdir.toString());
    File[] listfiles = folder.listFiles();
    for (int idx = 0; idx < listfiles.length; ++idx) {
      if (listfiles[idx].isFile()
              && listfiles[idx].getName().startsWith(MneConfigHelper.getBaseOutputName(m_conf, null))
              && listfiles[idx].getName().endsWith(MneConfigHelper.DEFAULT_FILE_EXTENSION)) {
        partfns.add(listfiles[idx].getName());
      }
    }
    Collections.sort(partfns); // keep the order for checksum
    for (int idx = 0; idx < partfns.size(); ++idx) {
      System.out.println(String.format("Verifying : %s", partfns.get(idx)));
      FileSplit split = new FileSplit(
              new Path(m_workdir, partfns.get(idx)), 0, 0L, new String[0]);
      InputFormat<NullWritable, MneDurableInputValue<DurableChunk<?>>> inputFormat =
              new MneInputFormat<MneDurableInputValue<DurableChunk<?>>, DurableChunk<?>>();
      RecordReader<NullWritable, MneDurableInputValue<DurableChunk<?>>> reader =
              inputFormat.getRecordReader(split, m_conf, null);
      MneDurableInputValue<DurableChunk<?>> dchkval = null;
      NullWritable dchkkey = reader.createKey();
      while (true) {
        dchkval = reader.createValue();
        if (reader.next(dchkkey, dchkval)) {
          byte b;
          for (int j = 0; j < dchkval.getValue().getSize(); ++j) {
            b = unsafe.getByte(dchkval.getValue().get() + j);
            cs.update(b);
          }
          tsize += dchkval.getValue().getSize();
          ++reccnt;
        } else {
          break;
        }
      }
      reader.close();
    }
    AssertJUnit.assertEquals(m_reccnt, reccnt);
    AssertJUnit.assertEquals(m_totalsize, tsize);
    AssertJUnit.assertEquals(m_checksum, cs.getValue());
    System.out.println(String.format("The checksum of chunk is %d", m_checksum));
  }

  @Test(enabled = true, dependsOnMethods = {"testWriteChunkData"})
  public void testBatchReadChunkDataUsingInputSession() throws Exception {
    List<String> partfns = new ArrayList<String>();
    long reccnt = 0L;
    long tsize = 0L;
    Checksum cs = new CRC32();
    cs.reset();
    File folder = new File(m_workdir.toString());
    File[] listfiles = folder.listFiles();
    for (int idx = 0; idx < listfiles.length; ++idx) {
      if (listfiles[idx].isFile()
              && listfiles[idx].getName().startsWith(MneConfigHelper.getBaseOutputName(m_conf, null))
              && listfiles[idx].getName().endsWith(MneConfigHelper.DEFAULT_FILE_EXTENSION)) {
        partfns.add(listfiles[idx].getName());
      }
    }
    Collections.sort(partfns); // keep the order for checksum
    List<Path> paths = new ArrayList<Path>();
    for (String fns : partfns) {
      paths.add(new Path(m_workdir, fns));
      System.out.println(String.format("[Batch Mode] Added : %s", fns));
    }
    MneDurableInputSession<DurableChunk<?>> m_session =
            new MneDurableInputSession<DurableChunk<?>>(m_tacontext, null,
                    paths.toArray(new Path[0]), MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX);
    SessionIterator<DurableChunk<?>, ?, Void, Void> m_iter = m_session.iterator();
    DurableChunk<?> val = null;
    while (m_iter.hasNext()) {
      val = m_iter.next();
      byte b;
      for (int j = 0; j < val.getSize(); ++j) {
        b = unsafe.getByte(val.get() + j);
        cs.update(b);
      }
      tsize += val.getSize();
      ++reccnt;
    }
    AssertJUnit.assertEquals(m_reccnt, reccnt);
    AssertJUnit.assertEquals(m_totalsize, tsize);
    AssertJUnit.assertEquals(m_checksum, cs.getValue());
    System.out.println(String.format("The checksum of chunk is %d [Batch Mode]", m_checksum));
  }
}
