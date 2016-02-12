package com.intel.mnemonic.service.allocatorservice;

import org.flowcomputing.commons.primitives.*;
import java.nio.ByteBuffer;

public interface VolatileMemoryAllocatorService {

    /**
     * Provide the service identifier for this allocator
     *
     * @return the service identifer of this allocator
     */
    public String getServiceId();

    /**
     * Initialize a memory pool through native interface backed by native
     * library.
     * 
     * @param capacity
     *            the capacity of memory pool
     * 
     * @param uri
     *            the location of memory pool will be created
     * 
     * @param isnew
     *            a place holder, always specify it as true
     *
     * @return the identifier of created memory pool
     */
    public long init(long capacity, String uri, boolean isnew);

    /**
     * close the memory pool through native interface.
     * 
     */
    public void close(long id);


    /**
     * force to synchronize uncommitted data to backed memory pool through
     * native interface.
     */
    public void sync(long id);

    /**
     * allocate specified size of memory block from backed memory pool.
     * 
     * @param id
     *            the identifier of backed memory pool
     * 
     * @param size
     *            specify size of memory block to be allocated
     * 
     * @return the address of allocated memory block from native memory pool
     */
    public long allocate(long id, long size, boolean initzero);

    /**
     * reallocate a specified size of memory block from backed memory pool.
     * 
     * @param id
     *            the identifier of backed memory pool
     * 
     * @param address
     *            the address of previous allocated memory block. it can be
     *            null.
     * 
     * @param size
     *            specify new size of memory block to be reallocated
     * 
     * @return the address of reallocated memory block from native memory pool
     */
    public long reallocate(long id, long address, long size, boolean initzero);

    /**
     * free a memory block by specify its address into backed memory pool.
     * 
     * @param id
     *            the identifier of backed memory pool
     * 
     * @param address
     *            the address of allocated memory block.
     */
    public void free(long id, long address);

    /**
     * create a ByteBuffer object which backed buffer is coming from backed
     * native memory pool.
     * 
     * @param id
     *            the identifier of backed memory pool
     * 
     * @param size
     *            the size of backed buffer that is managed by created
     *            ByteBuffer object.
     * 
     * @return a created ByteBuffer object with a backed native memory block
     */
    public ByteBuffer createByteBuffer(long id, long size);

    /**
     * resize a ByteBuffer object which backed buffer is coming from backed
     * native memory pool.
     * NOTE: the ByteBuffer object will be renewed and lost metadata e.g. position, mark and etc.
     * 
     * @param id
     *            the identifier of backed memory pool
     * 
     * @param bytebuf
     *            the specified ByteBuffer object to be destroyed
     *            
     * @param size
     *            the new size of backed buffer that is managed by created
     *            ByteBuffer object.
     * 
     * @return a created ByteBuffer object with a backed native memory block
     */
    public  ByteBuffer resizeByteBuffer(long id, ByteBuffer bytebuf, long size);
	
    /**
     * destroy a native memory block backed ByteBuffer object.
     * 
     * @param id
     *            the identifier of backed memory pool
     * 
     * @param bytebuf
     *            the specified ByteBuffer object to be destroyed
     */
    public void destroyByteBuffer(long id, ByteBuffer bytebuf);
}
