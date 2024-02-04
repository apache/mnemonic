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

/**
 * Implementation of the GeneralComputingService interface for sorting operations.
 */
public class SortServiceImpl implements GeneralComputingService {

    // Load native library when the class is loaded
    static {
        try {
            NativeLibraryLoader.loadFromJar("utilitiescomputing");
        } catch (Exception e) {
            throw new Error("Failed to load native library", e);
        }
    }

    /**
     * Get the service identifier.
     *
     * @return the service identifier
     */
    @Override
    public String getServiceId() {
        return "sort";
    }

    /**
     * Perform sorting operation based on the specified mode.
     *
     * @param mode     the sorting mode
     * @param valinfos an array of ValueInfo objects
     * @return an array of handler returned by native level
     */
    @Override
    public long[] perform(String mode, ValueInfo[] valinfos) {
        long[] ret = null;
        if (mode != null && valinfos != null) {
            if ("tensor_bubble".equals(mode)) {
                ret = nperformBubbleSort(valinfos);
            } else if ("1dlong_bubble".equals(mode)) {
                ret = nperform1DLongBubbleSort(valinfos);
            }
        }
        return ret;
    }

    /**
     * Perform sorting operation based on the specified mode, with additional parameters.
     *
     * @param mode     the sorting mode
     * @param valinfos an array of ValueInfo objects
     * @param dcHandler data container handler
     * @param dcSize    data container size
     * @return an array of handler returned by native level
     */
    @Override
    public long[] perform(String mode, ValueInfo[] valinfos, long dcHandler, long dcSize) {
        throw new UnsupportedOperationException("Invalid operation for sort.");
    }

    /**
     * Native function to perform bubble sort.
     *
     * @param valinfos an array of ValueInfo objects
     * @return an array of handler returned by native level
     */
    protected native long[] nperformBubbleSort(ValueInfo[] valinfos);

    /**
     * Native function to perform 1D long bubble sort.
     *
     * @param valinfos an array of ValueInfo objects
     * @return an array of handler returned by native level
     */
    protected native long[] nperform1DLongBubbleSort(ValueInfo[] valinfos);

}

