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

package org.apache.mnemonic.service.computingservice.internal;

import org.apache.mnemonic.service.computingservice.GeneralComputingService;
import org.flowcomputing.commons.primitives.NativeLibraryLoader;

public class PrintServiceImpl implements GeneralComputingService {
  static {
    try {
      NativeLibraryLoader.loadFromJar("utilitiescomputing");
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getServiceId() {
    return "print";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long perform(long handler, long[][] npf) {
    long ret = 0L;
    if (0L != handler && null != npf) {
      nperformPrint(handler, npf);
    }
    return ret;
  }

  protected native long nperformPrint(long handler, long[][] npf);

}
