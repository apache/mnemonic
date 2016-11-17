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

package org.apache.mnemonic.bench;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;

/**
 * Sort is the class of sort-bench.
 * 
 */
public class Sort {
  /**
   * Run workloads to bench performance.
   *
   * @param args
   *          array of commandline parameters
   */

  private static final Logger LOGGER = LoggerFactory.getLogger(Sort.class);

  public static void main(String[] args) throws Exception {

    Options options = new Options();

    Option mode = new Option("m", "mode", true, "run mode [A|B]");
    mode.setRequired(true);
    options.addOption(mode);

    Option input = new Option("i", "input", true, "input file path");
    input.setRequired(true);
    options.addOption(input);

    Option output = new Option("o", "output", true, "output file");
    output.setRequired(true);
    options.addOption(output);

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd;

    String runMode, inputFilePath, outputFilePath;

    try {
        cmd = parser.parse(options, args);
        runMode = cmd.getOptionValue("mode");
        inputFilePath = cmd.getOptionValue("input");
        outputFilePath = cmd.getOptionValue("output");
        if (!runMode.equals("A") && !runMode.equals("B")) {
          throw new ParseException("Run mode is not specified correctly, Please use A or B as run mode.");
        }
    } catch (ParseException e) {
        System.out.println(e.getMessage());
        formatter.printHelp("Sort-bench", options);
        System.exit(1);
        return;
    }
    MDC.put("mode", String.format("Mode-%s", runMode));

    LOGGER.info("Run Mode : {}", runMode);
    LOGGER.info("Input file : {}", inputFilePath);
    LOGGER.info("Output file : {}", outputFilePath);

    File inputFile = new File(inputFilePath);
    File outputFile = new File(outputFilePath);

    BufferedReader reader = null;
    BufferedWriter writer = null;

    TextFileSort tfsorter = null;

    long sttime;

    try {
      reader = new BufferedReader(new FileReader(inputFile));
      writer = new BufferedWriter(new FileWriter(outputFile));
      if (runMode.equals("A")) {
        /* regular way */
        tfsorter = new RegularTestFileSort();
      } else {
        /* mnemonic way */
        tfsorter = new DNCSTextFileSort();
      }

      sttime = System.nanoTime();
      tfsorter.load(reader);
      reportElapse("Load Time", sttime, System.nanoTime());
      sttime = System.nanoTime();
      tfsorter.doSort();
      reportElapse("Sort Time", sttime, System.nanoTime());
      sttime = System.nanoTime();
      tfsorter.store(writer);
      reportElapse("Store Time", sttime, System.nanoTime());
      reportSortInfo(tfsorter.getSortInfo());
      tfsorter.clear();
    } catch (FileNotFoundException e) {
      System.err.println(e.getMessage());
      throw e;
    } catch (IOException e) {
      System.err.println(e.getMessage());
      throw e;
    } finally {
      try {
        if (null != reader) {
          reader.close();
        }
        if (null != writer) {
          writer.close();
        }
      } catch (IOException e) {
        System.err.println(e.getMessage());
        throw e;
      }
    }
  }

  static void reportElapse(String msg, long t1, long t2) {
    LOGGER.info("{} : {} ms.", msg,
        TimeUnit.NANOSECONDS.toMillis(t2 - t1));
  }

  static void reportSortInfo(long[] sortinfo) {
    LOGGER.info("Scan Count : {} ", sortinfo[0]);
    LOGGER.info("Swap Count : {} ", sortinfo[1]);
    LOGGER.info("No Swap Count : {} ", sortinfo[2]);
  }
}
