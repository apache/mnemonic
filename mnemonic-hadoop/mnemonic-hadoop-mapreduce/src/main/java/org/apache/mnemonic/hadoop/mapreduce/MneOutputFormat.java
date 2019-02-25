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

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.mnemonic.hadoop.MneDurableOutputValue;

/**
 * A Mnemonic output format that satisfies the org.apache.hadoop.mapreduce API.
 */
public class MneOutputFormat<MV extends MneDurableOutputValue<?>>
        extends FileOutputFormat<NullWritable, MV> {

  @Override
  public RecordWriter<NullWritable, MV> getRecordWriter(TaskAttemptContext taskAttemptContext) throws IOException {
    return new MneMapreduceRecordWriter<MV>();
  }

}
