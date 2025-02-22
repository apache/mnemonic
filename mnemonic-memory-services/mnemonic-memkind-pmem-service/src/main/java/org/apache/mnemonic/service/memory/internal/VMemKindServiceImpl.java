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

package org.apache.mnemonic.service.memory.internal;

import org.apache.mnemonic.query.memory.EntityInfo;
import org.apache.mnemonic.query.memory.ResultSet;
import org.apache.mnemonic.service.computing.ValueInfo;
import org.apache.mnemonic.service.memory.MemoryServiceFeature;
import org.apache.mnemonic.service.memory.VolatileMemoryAllocatorService;
import org.apache.mnemonic.resgc.ReclaimContext;
import org.apache.mnemonic.primitives.NativeLibraryLoader;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * VMemKindServiceImpl is an implementation of the VolatileMemoryAllocatorService.
 * It provides volatile memory allocation using the vmemkind library.
 */
public class VMemKindServiceImpl implements VolatileMemoryAllocatorService {
    
    /** Flag to track if the native library has been loaded. */
    private static boolean nativeLoaded = false;

    /**
     * Loads the native vmemkind library required for memory operations.
     * This function ensures that the library is only loaded once.
     */
    static void loadNativeLibrary() {
        try {
            NativeLibraryLoader.loadFromJar("vmemkindallocator");
        } catch (Exception e) {
            throw new Error(e);
        }
        nativeLoaded = true;
    }

    /** A thread-safe map to track memory allocations and their capacities. */
    protected Map<Long, Long> m_info = Collections.synchronizedMap(new HashMap<Long, Long>());

    /**
     * Returns the unique identifier for this memory service.
     *
     * @return Service ID as a string.
     */
    @Override
    public String getServiceId() {
        return "vmem";
    }

    /**
     * Initializes a volatile memory space with a given capacity.
     * Loads the native library if it has not been loaded.
     *
     * @param capacity The size of the memory region.
     * @param uri The URI identifier (not used in volatile memory).
     * @param isnew Flag indicating whether a new region is created.
     * @return The identifier of the allocated memory space.
     */
    @Override
    public long init(long capacity, String uri, boolean isnew) {
        if (!nativeLoaded) {
            loadNativeLibrary();
        }
        long ret = ninit(capacity, uri, isnew);
        m_info.put(ret, capacity);
        return ret;
    }

    /**
     * Adjusts the capacity of the memory region.
     * This operation is not supported for volatile memory.
     *
     * @throws UnsupportedOperationException Always throws an exception.
     */
    @Override
    public long adjustCapacity(long id, long reserve) {
        throw new UnsupportedOperationException("Unsupported to reduce capacity of this memory service");
    }

    /**
     * Closes a memory region.
     *
     * @param id The identifier of the memory region.
     */
    @Override
    public void close(long id) {
        nclose(id);
    }

    /**
     * Synchronizes volatile memory to ensure consistency.
     *
     * @param id The identifier of the memory region.
     * @param addr The starting address of the memory to be synchronized.
     * @param length The length of the memory to synchronize.
     * @param autodetect Flag to enable automatic detection of memory regions.
     */
    @Override
    public void syncToVolatileMemory(long id, long addr, long length, boolean autodetect) {
        nsync(id, addr, length, autodetect);
    }

    /**
     * Retrieves the allocated capacity of a memory region.
     *
     * @param id The identifier of the memory region.
     * @return The capacity in bytes.
     */
    @Override
    public long capacity(long id) {
        return m_info.get(id);
    }

    /**
     * Allocates memory in the specified region.
     *
     * @param id The identifier of the memory region.
     * @param size The size of the memory to allocate.
     * @param initzero Flag indicating whether to initialize memory to zero.
     * @return The address of the allocated memory.
     */
    @Override
    public long allocate(long id, long size, boolean initzero) {
        return nallocate(id, size, initzero);
    }

    /**
     * Retrieves supported memory service features.
     *
     * @return A set of memory service features.
     */
    @Override
    public Set<MemoryServiceFeature> getFeatures() {
        Set<MemoryServiceFeature> ret = new HashSet<>();
        ret.add(MemoryServiceFeature.VOLATILE);
        return ret;
    }

    /**
     * Retrieves a direct byte buffer from the memory region.
     *
     * @param id The memory region identifier.
     * @param size The size of the buffer.
     * @return The allocated ByteBuffer.
     */
    @Override
    public ByteBuffer createByteBuffer(long id, long size) {
        return ncreateByteBuffer(id, size);
    }

    /**
     * Handles unsupported transaction operations by throwing an exception.
     */
    @Override
    public void beginTransaction(boolean readOnly) {
        throw new UnsupportedOperationException("Not support transaction");
    }

    @Override
    public void commitTransaction() {
        throw new UnsupportedOperationException("Not support transaction");
    }

    @Override
    public void abortTransaction() {
        throw new UnsupportedOperationException("Not support transaction");
    }

    @Override
    public boolean isInTransaction() {
        throw new UnsupportedOperationException("Not support transaction");
    }

    /**
     * Native method declarations for interacting with the underlying memory system.
     * These methods are implemented in the native library.
     */
    protected native long ninit(long capacity, String uri, boolean isnew);
    protected native void nclose(long id);
    protected native void nsync(long id, long addr, long length, boolean autodetect);
    protected native long nallocate(long id, long size, boolean initzero);
    protected native ByteBuffer ncreateByteBuffer(long id, long size);

    /* Optional Queryable Service */
    
    @Override
    public String[] getClassNames(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getEntityNames(long id, String clsname) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityInfo getEntityInfo(long id, String clsname, String etyname) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createEntity(long id, EntityInfo entityinfo) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroyEntity(long id, String clsname, String etyname) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateQueryableInfo(long id, String clsname, String etyname, ValueInfo updobjs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteQueryableInfo(long id, String clsname, String etyname, ValueInfo updobjs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResultSet query(long id, String querystr) {
        throw new UnsupportedOperationException();
    }
}
