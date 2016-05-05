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

package org.apache.mnemonic;

/**
 * a dummy object that is used for other test cases.
 * 
 * 
 *
 */
public class Payload implements java.io.Serializable, Comparable<Payload> {

  private static final long serialVersionUID = 187397440699436500L;

  public Payload(int iv, String strv, double dv) {
    ival = iv;
    strval = strv;
    dval = dv;
  }

  public int ival;
  public String strval;
  public double dval;

  @Override
  public int compareTo(Payload pl) {
    return ival == pl.ival && strval.equals(pl.strval) && dval == pl.dval ? 0 : 1;
  }
}
