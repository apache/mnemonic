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
import java.util.Random;

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
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.Utils;
import org.apache.mnemonic.hadoop.MneConfigHelper;
import org.apache.mnemonic.hadoop.mapreduce.MneInputFormat;
import org.apache.mnemonic.hadoop.mapreduce.MneMapreduceRecordWriter;
import org.apache.mnemonic.hadoop.mapreduce.MneOutputFormat;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class MneMapreduceIOTest {
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
        System.getProperty("test.tmp.dir", "target" + File.separator + "test" + File.separator + "tmp"));
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

    m_conf.set("mapreduce.output.fileoutputformat.outputdir", m_workdir.toString());

    MneConfigHelper.setInputMemServiceName(m_conf, "pmalloc");
    MneConfigHelper.setInputSlotKeyId(m_conf, 3L);
    MneConfigHelper.setInputDurableTypes(m_conf, new DurableType[] {DurableType.DURABLE});
    MneConfigHelper.setInputEntityFactoryProxies(m_conf, new Class<?>[] {PersonListEFProxy.class});
    MneConfigHelper.setOutputMemServiceName(m_conf, "pmalloc");
    MneConfigHelper.setOutputSlotKeyId(m_conf, 3L);
    MneConfigHelper.setOutputMemPoolSize(m_conf, 1024L * 1024 * 1024 * 4);
    MneConfigHelper.setOutputDurableTypes(m_conf, new DurableType[] {DurableType.DURABLE});
    MneConfigHelper.setOutputEntityFactoryProxies(m_conf, new Class<?>[] {PersonListEFProxy.class});

  }

  @AfterClass
  public void tearDown() {

  }

  @Test(enabled = true)
  public void testWritePersonData() throws Exception {
    NullWritable nada = NullWritable.get();
    OutputFormat<NullWritable, Person<Long>> outputFormat = new MneOutputFormat<Person<Long>>();
    RecordWriter<NullWritable, Person<Long>> writer = outputFormat.getRecordWriter(m_tacontext);
    Person<Long> person = null;
    for (int i = 0; i < m_reccnt; ++i) {
      person = ((MneMapreduceRecordWriter<Person<Long>>) writer).newDurableObjectRecord();
      person.setAge((short) m_rand.nextInt(50));
      person.setName(String.format("Name: [%s]", Utils.genRandomString()), true);
      m_sumage += person.getAge();
      writer.write(nada, person);
    }
    writer.close(m_tacontext);
  }

  @Test(enabled = true, dependsOnMethods = { "testWritePersonData" })
  public void testReadPersonData() throws Exception {
    long sumage = 0L;
    long reccnt = 0L;
    File folder = new File(m_workdir.toString());
    File[] listfiles = folder.listFiles();
    for (int idx = 0; idx < listfiles.length; ++idx) {
      if (listfiles[idx].isFile()
          && listfiles[idx].getName().endsWith(MneConfigHelper.FILE_EXTENSION)) {
        System.out.println(String.format("Verifying : %s", listfiles[idx].getName()));
        FileSplit split = new FileSplit(
            new Path(m_workdir, listfiles[idx].getName()), 0, 0L, new String[0]);
        InputFormat<NullWritable, Person<Long>> inputFormat = new MneInputFormat<Person<Long>>();
        RecordReader<NullWritable, Person<Long>> reader = inputFormat.createRecordReader(
            split, m_tacontext);
        Person<Long> person = null;
        while (reader.nextKeyValue()) {
          person = reader.getCurrentValue();
          AssertJUnit.assertTrue(person.getAge() < 51);
          sumage += person.getAge();
          ++reccnt;
        }
        reader.close();
      }
    }
    AssertJUnit.assertEquals(m_sumage, sumage);
    AssertJUnit.assertEquals(m_reccnt, reccnt);
    System.out.println(String.format("The sum of ages is %d", sumage));
  }
}
