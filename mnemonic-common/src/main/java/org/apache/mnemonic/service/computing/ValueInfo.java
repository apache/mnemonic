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

package org.apache.mnemonic.service.computing;

import org.apache.mnemonic.DurableType;

public class ValueInfo {

  /**
   * a handler to a object graph
   */
  public long handler;
  
  /**
   * a table for native address mapping
   */
  public long[][] transtable;

  /**
   * an array of memory service functions
   */
  public long[] memfuncs;

  /**
   * a list of frame of native parameter
   */
  public long[][] frames;

  /**
   * a type of this specified value
   */
  public DurableType dtype;

}
