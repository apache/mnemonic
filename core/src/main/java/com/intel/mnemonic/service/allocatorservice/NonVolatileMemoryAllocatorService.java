package com.intel.mnemonic.service.allocatorservice;

import org.flowcomputing.commons.primitives.*;
import java.nio.ByteBuffer;

public interface NonVolatileMemoryAllocatorService extends VolatileMemoryAllocatorService {

    /**
     * retrieve a bytebuffer from its handler
     *
     * @param id
     *            the identifier of backed memory pool
     * 
     * @param handler
     *            the handler of a nonvolatile bytebuffer
     *
     * @return the nonvolatile bytebuffer
     *
     */
    public ByteBuffer retrieveByteBuffer(long id, long handler);

    /**
     * retrieve the size of a nonvolatile memory object
     *
     * @param id
     *            the identifier of backed memory pool
     * 
     * @param handler
     *            the handler of a nonvolatile object
     *
     * @return the size of nonvolatile object
     *
     */
    public long retrieveSize(long id, long handler);
	
    /**
     * get the handler of a nonvolatile bytebuffer
     *
     * @param id
     *            the identifier of backed memory pool
     * 
     * @param buf
     *            the nonvolatile bytebuffer
     *
     * @return the handler of this specified nonvolatile bytebuffer
     *
     */
    public long getByteBufferHandler(long id, ByteBuffer buf);

    /**
     * set a handler to a key.
     * 
     * @param id
     *            the identifier of backed memory pool
     * 
     * @param key
     *            the key to set this handler
     *            
     * @param handler
     *            the handler
     */
    public void setHandler(long id, long key, long handler);
	
    /**
     * get a handler from specified key.
     * 
     * @param id
     *            the identifier of backed memory pool
     * 
     * @param key
     *            the key to get its handler
     *            
     * @return the handler of the specified key
     */
    public long getHandler(long id, long key);
	
    /**
     * return the number of available keys to use.
     * 
     * @param id
     *            the identifier of backed memory pool
     *            
     * @return the number of keys
     */
    public long handlerCapacity(long id);
		
    /**
     * return the base address of this persistent memory pool.
     * 
     * @param id
     *            the identifier of backed memory pool
     *            
     * @return the base address of this pmem pool
     */
    public long getBaseAddress(long id);

}
