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

/**
 * load native library from itself in Jar or file system.
 * 
 * @author wg
 *
 */
public final class NativeLibraryLoader {
    
    private static final int COPYING_BUFFER_LENGTH = 40960;
    
    /**
     * private constructor that prevents it from instantiating.
     */
    private NativeLibraryLoader() {

    }
    
    /**
     * retrieve the extension of native library specific to the underlying
     * system.
     * 
     * @return the extension of library
     */
    public static String getNativeLibraryExtension() {
        String libname = System.mapLibraryName("dummylib");
        int idx = libname.lastIndexOf(".");
        return idx < 0 ? null : libname.substring(idx);
    }
    
    /**
     * retrieve the suffix of native library
     * 
     * @return the suffix of extension of library
     */
    public static String getNativeLibraryExtSuffix() {
        String ext = getNativeLibraryExtension();
        return null == ext ? null : ext.substring(1);
    }
    
    /**
     * load native library from itself in jar package
     * 
     * @param libname
     *            the name of native library without prefix and suffix
     * 
     * @see #loadFromJar(String, String, File)
     */
    public static void loadFromJar(String libname) throws IOException {
        loadFromJar(libname, "/native", null);
    }
    
    /**
     * load native library from itself in jar package
     * 
     * @param libname
     *            the name of native library without prefix and suffix
     * 
     * @param libdir
     *            the directory in Jar package that contains this native library
     * 
     * @see #loadFromJar(String, String, File)
     */
    public static void loadLibraryFromJar(String libname, String libdir)
            throws IOException {
        loadFromJar(libname, libdir, null);
    }
    
    /**
     * load native library from itself in jar package
     * 
     * @param libname
     *            the name of native library without prefix and suffix
     * 
     * @param libdir
     *            the directory in Jar package that contains this native library
     *
     * @param tmpdir
     *            the directory which is used to temporarily store the specified
     *            native library
     */
    public static void loadFromJar(String libname, String libdir, File tmpdir)
            throws IOException {
        
        if (!libname.trim().isEmpty()) {
            
            String pathname = String.format("%s/%s", libdir,
                    System.mapLibraryName(libname));
            
            String tmpfprefix = libname;
            String tmpfsuffix = getNativeLibraryExtension();
            tmpfsuffix = null == tmpfsuffix ? "" : tmpfsuffix;
            
            File temp = File.createTempFile(tmpfprefix, tmpfsuffix, tmpdir);
            temp.deleteOnExit();
            
            if (!temp.exists()) {
                throw new FileNotFoundException("The tempfile "
                        + temp.getAbsolutePath() + " does not exist.");
            }
            
            byte[] buffer = new byte[COPYING_BUFFER_LENGTH];
            int readBytes;
            
            InputStream is = NativeLibraryLoader.class
                .getResourceAsStream(pathname);
            if (is == null) {
                throw new FileNotFoundException("The library " + pathname
                        + " was not found inside jar file.");
            }
            OutputStream os = new FileOutputStream(temp);
            
            try {
                while ((readBytes = is.read(buffer)) != -1) {
                    os.write(buffer, 0, readBytes);
                }
            } finally {
                os.close();
                is.close();
            }
            
            System.load(temp.getAbsolutePath());
        
        } else {
            throw new RuntimeException("Library name is not specified.");
        }
    }
    
    /**
     * load native library from current directory in file system.
     * 
     * @param libname
     *            the name of native library without prefix and suffix
     * 
     * @see #loadFromFileSystem(String, String)
     */
    public static void loadFromFileSystem(String libname) {
        loadFromFileSystem(libname, ".");
    }
    
    /**
     * load native library from specified directory in file system.
     * 
     * @param libname
     *            the name of native library without prefix and suffix
     * 
     * @param libdir
     *            the directory in file system that contains this native library
     * 
     */
    public static void loadFromFileSystem(String libname, String libdir) {
        
        if (!libname.trim().isEmpty()) {
            
            String pathname = String.format("%s/%s", libdir,
                    System.mapLibraryName(libname));
            
            System.load(pathname);
        
        } else {
            throw new RuntimeException("Library name is not specified.");
        }
    }
}
