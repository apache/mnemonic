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

package org.apache.mnemonic.mapreduce;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.apache.commons.lang3.RandomUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.TaskAttemptID;
import org.apache.hadoop.mapreduce.TaskType;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.task.TaskAttemptContextImpl;
import org.apache.mnemonic.DurableBuffer;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.Utils;
import org.apache.mnemonic.hadoop.MneConfigHelper;
import org.apache.mnemonic.hadoop.MneDurableInputValue;
import org.apache.mnemonic.hadoop.MneDurableOutputSession;
import org.apache.mnemonic.hadoop.MneDurableOutputValue;
import org.apache.mnemonic.hadoop.mapreduce.MneInputFormat;
import org.apache.mnemonic.hadoop.mapreduce.MneOutputFormat;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class MneMapreduceBufferDataTest {

  private static final String DEFAULT_BASE_WORK_DIR = "target" + File.separator + "test" + File.separator + "tmp";
  private static final String DEFAULT_WORK_DIR = DEFAULT_BASE_WORK_DIR + File.separator + "buffer-data";
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
  private List<String> m_partfns;

  @BeforeClass
  public void setUp() throws IOException {
    m_workdir = new Path(
        System.getProperty("test.tmp.dir", DEFAULT_WORK_DIR));
    m_conf = new JobConf();
    m_rand = Utils.createRandom();
    m_partfns = new ArrayList<String>();

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
    MneConfigHelper.setBaseOutputName(m_conf, null, "buffer-data");

    MneConfigHelper.setMemServiceName(m_conf, MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX, SERVICE_NAME);
    MneConfigHelper.setSlotKeyId(m_conf, MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX, SLOT_KEY_ID);
    MneConfigHelper.setDurableTypes(m_conf,
        MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX, new DurableType[] {DurableType.BUFFER});
    MneConfigHelper.setEntityFactoryProxies(m_conf,
        MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX, new Class<?>[] {});
    MneConfigHelper.setMemServiceName(m_conf, MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX, SERVICE_NAME);
    MneConfigHelper.setSlotKeyId(m_conf, MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX, SLOT_KEY_ID);
    MneConfigHelper.setMemPoolSize(m_conf,
        MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX, 1024L * 1024 * 1024 * 4);
    MneConfigHelper.setDurableTypes(m_conf,
        MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX, new DurableType[] {DurableType.BUFFER});
    MneConfigHelper.setEntityFactoryProxies(m_conf,
        MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX, new Class<?>[] {});
  }

  @AfterClass
  public void tearDown() {

  }

  protected DurableBuffer<?> genupdDurableBuffer(
      MneDurableOutputSession<DurableBuffer<?>> s, Checksum cs) {
    DurableBuffer<?> ret = null;
    int sz = m_rand.nextInt(1024 * 1024) + 1024 * 1024;
    ret = s.newDurableObjectRecord(sz);
    if (null != ret) {
      ret.get().clear();
      byte[] rdbytes = RandomUtils.nextBytes(sz);
      Assert.assertNotNull(rdbytes);
      ret.get().put(rdbytes);
      cs.update(rdbytes, 0, rdbytes.length);
      m_totalsize += sz;
    }
    return ret;
  }

  @Test(enabled = true)
  public void testWriteBufferData() throws Exception {
    NullWritable nada = NullWritable.get();
    MneDurableOutputSession<DurableBuffer<?>> sess =
        new MneDurableOutputSession<DurableBuffer<?>>(m_tacontext, null,
            MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX);
    MneDurableOutputValue<DurableBuffer<?>> mdvalue =
        new MneDurableOutputValue<DurableBuffer<?>>(sess);
    OutputFormat<NullWritable, MneDurableOutputValue<DurableBuffer<?>>> outputFormat =
        new MneOutputFormat<MneDurableOutputValue<DurableBuffer<?>>>();
    RecordWriter<NullWritable, MneDurableOutputValue<DurableBuffer<?>>> writer =
        outputFormat.getRecordWriter(m_tacontext);
    DurableBuffer<?> dbuf = null;
    Checksum cs = new CRC32();
    cs.reset();
    for (int i = 0; i < m_reccnt; ++i) {
      dbuf = genupdDurableBuffer(sess, cs);
      Assert.assertNotNull(dbuf);
      writer.write(nada, mdvalue.of(dbuf));
    }
    m_checksum = cs.getValue();
    writer.close(m_tacontext);
    sess.close();
  }

  @Test(enabled = true, dependsOnMethods = { "testWriteBufferData" })
  public void testReadBufferData() throws Exception {
    long reccnt = 0L;
    long tsize = 0L;
    byte[] buf;
    Checksum cs = new CRC32();
    cs.reset();
    File folder = new File(m_workdir.toString());
    File[] listfiles = folder.listFiles();
    for (int idx = 0; idx < listfiles.length; ++idx) {
      if (listfiles[idx].isFile()
          && listfiles[idx].getName().startsWith(MneConfigHelper.getBaseOutputName(m_conf, null))
          && listfiles[idx].getName().endsWith(MneConfigHelper.DEFAULT_FILE_EXTENSION)) {
        m_partfns.add(listfiles[idx].getName());
      }
    }
    Collections.sort(m_partfns); // keep the order for checksum
    for (int idx = 0; idx < m_partfns.size(); ++idx) {
      System.out.println(String.format("Verifying : %s", m_partfns.get(idx)));
      FileSplit split = new FileSplit(
          new Path(m_workdir, m_partfns.get(idx)), 0, 0L, new String[0]);
      InputFormat<NullWritable, MneDurableInputValue<DurableBuffer<?>>> inputFormat =
          new MneInputFormat<MneDurableInputValue<DurableBuffer<?>>, DurableBuffer<?>>();
      RecordReader<NullWritable, MneDurableInputValue<DurableBuffer<?>>> reader =
          inputFormat.createRecordReader(split, m_tacontext);
      MneDurableInputValue<DurableBuffer<?>> dbufval = null;
      while (reader.nextKeyValue()) {
        dbufval = reader.getCurrentValue();
        assert dbufval.getValue().getSize() == dbufval.getValue().get().capacity();
        dbufval.getValue().get().clear();
        buf = new byte[dbufval.getValue().get().capacity()];
        dbufval.getValue().get().get(buf);
        cs.update(buf, 0, buf.length);
        tsize += dbufval.getValue().getSize();
        ++reccnt;
      }
      reader.close();
    }
    AssertJUnit.assertEquals(m_reccnt, reccnt);
    AssertJUnit.assertEquals(m_totalsize, tsize);
    AssertJUnit.assertEquals(m_checksum, cs.getValue());
    System.out.println(String.format("The checksum of buffer is %d", m_checksum));
  }
}
