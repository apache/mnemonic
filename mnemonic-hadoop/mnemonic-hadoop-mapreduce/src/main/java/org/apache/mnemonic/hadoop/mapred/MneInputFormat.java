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

package org.apache.mnemonic.hadoop.mapred;

import java.io.IOException;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.mnemonic.hadoop.MneDurableInputValue;

/**
 * A Mnemonic input format that satisfies the org.apache.hadoop.mapred API.
 * 
 * @param <MV> The type parameter representing the mnemonic durable input value.
 * @param <V> The type parameter representing the value.
 */
public class MneInputFormat<MV extends MneDurableInputValue<V>, V> extends FileInputFormat<NullWritable, MV> {

  /**
   * Creates a RecordReader for the given InputSplit.
   *
   * @param inputSplit The input split containing the information about the file to read.
   * @param jobConf The job configuration containing the job settings.
   * @param reporter The reporter to report progress, status updates, etc.
   * @return A RecordReader to read the records from the input split.
   * @throws IOException If an error occurs while creating the RecordReader.
   */
  @Override
  public RecordReader<NullWritable, MV> getRecordReader(InputSplit inputSplit, JobConf jobConf, Reporter reporter) 
      throws IOException {
    // Cast the input split to a FileSplit to work with file-based input splits
    FileSplit fileSplit = (FileSplit) inputSplit;

    // Create a new instance of MneMapredRecordReader with the given file split and job configuration
    MneMapredRecordReader<MV, V> reader = new MneMapredRecordReader<MV, V>(fileSplit, jobConf);

    // Return the created RecordReader
    return reader;
  }

}
