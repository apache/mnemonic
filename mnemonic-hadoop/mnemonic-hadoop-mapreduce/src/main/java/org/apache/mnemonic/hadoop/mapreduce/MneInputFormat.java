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

import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.mnemonic.hadoop.MneDurableInputValue;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

import org.apache.hadoop.io.NullWritable;

/**
 * A Mnemonic input format that satisfies the org.apache.hadoop.mapreduce API.
 */
public class MneInputFormat<MV extends MneDurableInputValue<V>, V> extends FileInputFormat<NullWritable, MV> {

  @Override
  protected boolean isSplitable(JobContext context, Path filename) {
    return false;
  }

  @Override
  public RecordReader<NullWritable, MV> createRecordReader(InputSplit inputSplit,
                     TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException {
    MneMapreduceRecordReader<MV, V> reader = new MneMapreduceRecordReader<MV, V>();
    reader.initialize(inputSplit, taskAttemptContext);
    return reader;
  }
}
