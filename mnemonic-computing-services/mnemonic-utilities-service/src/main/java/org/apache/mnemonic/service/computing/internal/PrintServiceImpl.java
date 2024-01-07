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

package org.apache.mnemonic.service.computing.internal;

import org.apache.mnemonic.service.computing.GeneralComputingService;
import org.apache.mnemonic.service.computing.ValueInfo;
import org.apache.mnemonic.primitives.NativeLibraryLoader;

public class PrintServiceImpl implements GeneralComputingService {

    // Load the native library when the class is loaded
    static {
        try {
            NativeLibraryLoader.loadFromJar("utilitiescomputing");
        } catch (Exception e) {
            // If there's an error loading the native library, throw an Error
            throw new Error("Failed to load native library", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServiceId() {
        // Return the unique identifier for this computing service
        return "print";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] perform(String mode, ValueInfo[] valinfos) {
        long[] ret = null;
        if (valinfos != null) {
            // Call the native method to perform the print action
            ret = nperformPrint(valinfos);
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] perform(String mode, ValueInfo[] valinfos, long dcHandler, long dcSize) {
        // This operation is not supported for print, so throw an UnsupportedOperationException
        throw new UnsupportedOperationException("Invalid operation for print.");
    }

    /**
     * A native function to fulfill the action of print at the native level.
     *
     * @param valinfos an array of value info, some of them could be set as NULL
     * @return an array of handler returned by native level
     */
    protected native long[] nperformPrint(ValueInfo[] valinfos);

}

