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

package org.apache.mnemonic.primitives;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * A utility class to load native libraries from the classpath or file system.
 * This class can load native libraries from JAR resources or from the local file system.
 * It provides methods to load libraries by name, directory, and temporary directory.
 *
 * @author wg
 */
public final class NativeLibraryLoader {

    private static final int COPYING_BUFFER_LENGTH = 40960;

    private NativeLibraryLoader() {
        // private constructor to prevent instantiation
    }

    /**
     * Retrieve the extension of a native library specific to the underlying system.
     *
     * @return the extension of the library
     */
    public static String getNativeLibraryExtension() {
        String libName = System.mapLibraryName("dummylib");
        int lastDotIndex = libName.lastIndexOf(".");
        return lastDotIndex < 0 ? null : libName.substring(lastDotIndex);
    }

    /**
     * Retrieve the suffix of the native library extension.
     *
     * @return the suffix of the extension of the library
     */
    public static String getNativeLibraryExtSuffix() {
        String ext = getNativeLibraryExtension();
        return Objects.requireNonNullElse(ext, "").substring(1);
    }

    /**
     * Load a native library from the JAR package.
     *
     * @param libName the name of the native library without prefix and suffix
     * @throws IOException if an I/O error occurs
     */
    public static void loadFromJar(String libName) throws IOException {
        loadFromJar(libName, "/native", null);
    }

    /**
     * Load a native library from the JAR package.
     *
     * @param libName the name of the native library without prefix and suffix
     * @param libDir  the directory in the JAR package that contains this native library
     * @throws IOException if an I/O error occurs
     */
    public static void loadLibraryFromJar(String libName, String libDir) throws IOException {
        loadFromJar(libName, libDir, null);
    }

    /**
     * Load a native library from the JAR package.
     *
     * @param libName the name of the native library without prefix and suffix
     * @param libDir  the directory in the JAR package that contains this native library
     * @param tmpDir  the directory used to temporarily store the specified native library
     * @throws IOException if an I/O error occurs
     */
    public static void loadFromJar(String libName, String libDir, File tmpDir) throws IOException {
        if (libName.trim().isEmpty()) {
            throw new RuntimeException("Library name is not specified.");
        }

        String pathname = String.format("%s/%s", libDir, System.mapLibraryName(libName));
        String tmpFilePrefix = libName;
        String tmpFileSuffix = getNativeLibraryExtension();
        tmpFileSuffix = Objects.requireNonNullElse(tmpFileSuffix, "");

        File temp = File.createTempFile(tmpFilePrefix, tmpFileSuffix, tmpDir);
        temp.deleteOnExit();

        if (!temp.exists()) {
            throw new FileNotFoundException("The tempfile " + temp.getAbsolutePath() + " does not exist.");
        }

        byte[] buffer = new byte[COPYING_BUFFER_LENGTH];
        int readBytes;

        try (InputStream is = NativeLibraryLoader.class.getResourceAsStream(pathname);
             OutputStream os = new FileOutputStream(temp)) {

            if (is == null) {
                throw new FileNotFoundException("The library " + pathname + " was not found inside the JAR file.");
            }

            while ((readBytes = is.read(buffer)) != -1) {
                os.write(buffer, 0, readBytes);
            }
        }

        System.load(temp.getAbsolutePath());
    }

    /**
     * Load a native library from the current directory in the file system.
     *
     * @param libName the name of the native library without prefix and suffix
     */
    public static void loadFromFileSystem(String libName) {
        loadFromFileSystem(libName, ".");
    }

    /**
     * Load a native library from the specified directory in the file system.
     *
     * @param libName the name of the native library without prefix and suffix
     * @param libDir  the directory in the file system that contains this native library
     */
    public static void loadFromFileSystem(String libName, String libDir) {
        if (libName.trim().isEmpty()) {
            throw new RuntimeException("Library name is not specified.");
        }

        String pathname = String.format("%s/%s", libDir, System.mapLibraryName(libName));
        System.load(pathname);
    }
}
