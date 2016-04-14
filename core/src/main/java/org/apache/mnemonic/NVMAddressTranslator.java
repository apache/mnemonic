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
 * translate persistent memory address for allocator
 *
 */
public interface NVMAddressTranslator {

  /**
   * calculate the portable address
   *
   * @param addr
   *          the address to be calculated
   *
   * @return the portable address
   */
  long getPortableAddress(long addr);

  /**
   * calculate the effective address
   *
   * @param addr
   *          the address to be calculated
   *
   * @return the effective address
   */
  long getEffectiveAddress(long addr);

  /**
   * get the base address
   *
   * @return the base address
   */
  long getBaseAddress();

  /**
   * set the base address for calculation
   *
   * @param addr
   *          the base address
   *
   */
  long setBaseAddress(long addr);
}
