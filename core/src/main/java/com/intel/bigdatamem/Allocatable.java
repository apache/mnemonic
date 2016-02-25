package com.intel.bigdatamem;

/**
 * an interface to allocate memory resources from any underlying memory kind
 * of storage.
 * 
 */
public interface Allocatable<A extends CommonAllocator<A>> {
	
    /**
     * create a memory chunk that is managed by its holder.
     * 
     * @param size
     *            specify the size of memory chunk
     * 
     * @param autoreclaim
     * 	          specify whether or not to reclaim this
     *            chunk automatically
     *
     * @return a holder contains a memory chunk
     */
    public MemChunkHolder<A> createChunk(long size, boolean autoreclaim);

    /**
     * create a memory chunk that is managed by its holder.
     * 
     * @param size
     *            specify the size of memory chunk
     * 
     * @return a holder contains a memory chunk
     */
    public MemChunkHolder<A> createChunk(long size);

    /**
     * create a memory buffer that is managed by its holder.
     * 
     * @param size
     *            specify the size of memory buffer
     * 
     * @param autoreclaim
     * 	          specify whether or not to reclaim this
     *            buffer automatically
     *
     * @return a holder contains a memory buffer
     */
    public  MemBufferHolder<A> createBuffer(long size, boolean autoreclaim);
	
    /**
     * create a memory buffer that is managed by its holder.
     * 
     * @param size
     *            specify the size of memory buffer
     * 
     * @return a holder contains a memory buffer
     */
    public  MemBufferHolder<A> createBuffer(long size);

    /**
     * register a memory chunk for auto-reclaim
     *
     * @param mholder
     *           specify a chunk holder to register
     */
    public void registerChunkAutoReclaim(MemChunkHolder<A> mholder);

    /**
     * register a memory buffer for auto-reclaim
     *
     * @param mholder
     *           specify a buffer holder to register
     */
    public void registerBufferAutoReclaim(MemBufferHolder<A> mholder);

    /**
     * resize a memory chunk.
     * 
     * @param mholder
     *           specify a chunk holder for resizing
     * 
     * @param size
     *            specify a new size of this memory chunk
     * 
     * @return the resized memory chunk holder
     * 
     */
    public MemChunkHolder<A> resizeChunk(MemChunkHolder<A> mholder, long size);
	
    /**
     * resize a memory buffer.
     * 
     * @param mholder
     *           specify a buffer holder for resizing
     * 
     * @param size
     *            specify a new size of this memory buffer
     * 
     * @return the resized memory buffer holder
     * 
     */
    public MemBufferHolder<A> resizeBuffer(MemBufferHolder<A> mholder, long size);

}
