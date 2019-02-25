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
package org.apache.mnemonic.hadoop.mapreduce;

import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.mnemonic.CloseableIterator;
import org.apache.mnemonic.hadoop.MneConfigHelper;
import org.apache.mnemonic.hadoop.MneDurableInputSession;
import org.apache.mnemonic.hadoop.MneDurableInputValue;

/**
 * This record reader implements the org.apache.hadoop.mapreduce API.
 *
 * @param <V> the type of the data item
 */
public class MneMapreduceRecordReader<MV extends MneDurableInputValue<V>, V>
   extends org.apache.hadoop.mapreduce.RecordReader<NullWritable, MV> {

  protected CloseableIterator<V> m_iter;
  protected MneDurableInputSession<V> m_session;

  @Override
  public void close() throws IOException {
    if (null != m_iter) {
      m_iter.close();
    }
  }

  @Override
  public void initialize(InputSplit inputSplit, TaskAttemptContext context) {
    FileSplit split = (FileSplit) inputSplit;
    m_session = new MneDurableInputSession<V>(context, null, new Path[]{split.getPath()},
            MneConfigHelper.DEFAULT_INPUT_CONFIG_PREFIX);
    m_iter = m_session.iterator();
  }

  @Override
  public boolean nextKeyValue() throws IOException, InterruptedException {
    return m_iter.hasNext();
  }

  @Override
  public NullWritable getCurrentKey() throws IOException, InterruptedException {
    return NullWritable.get();
  }

  @SuppressWarnings("unchecked")
  @Override
  public MV getCurrentValue() throws IOException, InterruptedException {
    return (MV) new MneDurableInputValue<V>(m_session, m_iter.next());
  }

  @Override
  public float getProgress() throws IOException {
    return 0.5f; /* TBD */
  }

}
