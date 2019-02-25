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
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.Utils;
import org.apache.mnemonic.hadoop.MneConfigHelper;
import org.apache.mnemonic.hadoop.MneDurableInputValue;
import org.apache.mnemonic.hadoop.MneDurableOutputSession;
import org.apache.mnemonic.hadoop.MneDurableOutputValue;
import org.apache.mnemonic.hadoop.mapred.MneInputFormat;
import org.apache.mnemonic.hadoop.mapred.MneOutputFormat;
import org.apache.mnemonic.common.Person;
import org.apache.mnemonic.common.PersonListEFProxy;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public class MneMapredPersonDataTest {

  private static final String DEFAULT_BASE_WORK_DIR = "target" + File.separator + "test" + File.separator + "tmp";
  private static final String DEFAULT_WORK_DIR = DEFAULT_BASE_WORK_DIR + File.separator + "person-data";
  private static final String SERVICE_NAME = "pmalloc";
  private static final long SLOT_KEY_ID = 5L;
  private Path m_workdir;
  private JobConf m_conf;
  private FileSystem m_fs;
  private Random m_rand;
  private TaskAttemptID m_taid;
  private TaskAttemptContext m_tacontext;
  private long m_reccnt = 500000L;
  private long m_sumage = 0L;

  @BeforeClass
  public void setUp() throws IOException {
    m_workdir = new Path(
            System.getProperty("test.tmp.dir", DEFAULT_WORK_DIR));
    m_conf = new JobConf();
    m_rand = Utils.createRandom();

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
    MneConfigHelper.setBaseOutputName(m_conf, null, "person-data");

    MneConfigHelper.setMemServiceName(m_conf, MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX, SERVICE_NAME);
    MneConfigHelper.setSlotKeyId(m_conf, MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX, SLOT_KEY_ID);
    MneConfigHelper.setDurableTypes(m_conf,
            MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX, new DurableType[]{DurableType.DURABLE});
    MneConfigHelper.setEntityFactoryProxies(m_conf,
            MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX, new Class<?>[]{PersonListEFProxy.class});
    MneConfigHelper.setMemServiceName(m_conf, MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX, SERVICE_NAME);
    MneConfigHelper.setSlotKeyId(m_conf, MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX, SLOT_KEY_ID);
    MneConfigHelper.setMemPoolSize(m_conf,
            MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX, 1024L * 1024 * 1024 * 4);
    MneConfigHelper.setDurableTypes(m_conf,
            MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX, new DurableType[]{DurableType.DURABLE});
    MneConfigHelper.setEntityFactoryProxies(m_conf,
            MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX, new Class<?>[]{PersonListEFProxy.class});
  }

  @AfterClass
  public void tearDown() {

  }

  @Test(enabled = true)
  public void testWritePersonData() throws Exception {
    NullWritable nada = NullWritable.get();
    MneDurableOutputSession<Person<Long>> sess =
            new MneDurableOutputSession<Person<Long>>(m_tacontext, null,
                    MneConfigHelper.DEFAULT_OUTPUT_CONFIG_PREFIX);
    MneDurableOutputValue<Person<Long>> mdvalue =
            new MneDurableOutputValue<Person<Long>>(sess);
    OutputFormat<NullWritable, MneDurableOutputValue<Person<Long>>> outputFormat =
            new MneOutputFormat<MneDurableOutputValue<Person<Long>>>();
    RecordWriter<NullWritable, MneDurableOutputValue<Person<Long>>> writer =
            outputFormat.getRecordWriter(null, m_conf, null, null);
    Person<Long> person = null;
    for (int i = 0; i < m_reccnt; ++i) {
      person = sess.newDurableObjectRecord();
      person.setAge((short) m_rand.nextInt(50));
      person.setName(String.format("Name: [%s]", Utils.genRandomString()), true);
      m_sumage += person.getAge();
      writer.write(nada, mdvalue.of(person));
    }
    writer.close(null);
    sess.close();
  }

  @Test(enabled = true, dependsOnMethods = {"testWritePersonData"})
  public void testReadPersonData() throws Exception {
    long sumage = 0L;
    long reccnt = 0L;
    File folder = new File(m_workdir.toString());
    File[] listfiles = folder.listFiles();
    for (int idx = 0; idx < listfiles.length; ++idx) {
      if (listfiles[idx].isFile()
              && listfiles[idx].getName().startsWith(MneConfigHelper.getBaseOutputName(m_conf, null))
              && listfiles[idx].getName().endsWith(MneConfigHelper.DEFAULT_FILE_EXTENSION)) {
        System.out.println(String.format("Verifying : %s", listfiles[idx].getName()));
        FileSplit split = new FileSplit(
                new Path(m_workdir, listfiles[idx].getName()), 0, 0L, new String[0]);
        InputFormat<NullWritable, MneDurableInputValue<Person<Long>>> inputFormat =
                new MneInputFormat<MneDurableInputValue<Person<Long>>, Person<Long>>();
        RecordReader<NullWritable, MneDurableInputValue<Person<Long>>> reader =
                inputFormat.getRecordReader(split, m_conf, null);
        MneDurableInputValue<Person<Long>> personval = null;
        NullWritable personkey = reader.createKey();
        while (true) {
          personval = reader.createValue();
          if (reader.next(personkey, personval)) {
            AssertJUnit.assertTrue(personval.getValue().getAge() < 51);
            sumage += personval.getValue().getAge();
            ++reccnt;
          } else {
            break;
          }
        }
        reader.close();
      }
    }
    AssertJUnit.assertEquals(m_reccnt, reccnt);
    AssertJUnit.assertEquals(m_sumage, sumage);
    System.out.println(String.format("The checksum of ages is %d", sumage));
  }
}
