
package com.intel.bigdatamem;

/**
 * a interface to allocate big memory resource from any underlying memory kind
 * of storage.
 * 
 *
 */
public interface Allocatable<A extends CommonAllocator<A>> {
	
	/**
	 * create a MemChunkHolder object along with a memory chunk that is
	 * allocated from backed native memory pool.
	 * 
	 * @param <A> the type of bound allocator 
	 * 
	 * @param size
	 *            specify the size of memory chunk
	 * 
	 * @param autoreclaim
	 * 	          specify whether to reclaim its managed
	 *            chunk automatically
	 *
	 * @return a created MemChunkHolder object
	 * @see BigDataMemAllocator#createChunk(long)
	 */
	public MemChunkHolder<A> createChunk(long size, boolean autoreclaim);

	public MemChunkHolder<A> createChunk(long size);

	/**
	 * create a MemBufferHolder object along with a ByteBuffer which is backed
	 * with a buffer allocated from backed native memory pool.
	 * 
	 * @param <A> the type of bound allocator 
	 * 
	 * @param size
	 *            specify the size of backed memory buffer
	 * 
	 * @param autoreclaim
	 * 	          specify whether to reclaim its managed
	 *            buffer automatically
	 * 
	 * @return a created MemBufferHolder object
	 *
	 * @see BigDataMemAllocator#createBuffer(long)
	 */
	public  MemBufferHolder<A> createBuffer(long size, boolean autoreclaim);
	
	public  MemBufferHolder<A> createBuffer(long size);

	public void registerChunkAutoReclaim(MemChunkHolder<A> mholder);

	public void registerBufferAutoReclaim(MemBufferHolder<A> mholder);

	/**
	 * reallocate a specified size of memory block from backed memory pool.
	 * 
	 * @param address
	 *            the address of previous allocated memory block. it can be
	 *            null.
	 * 
	 * @param size
	 *            specify new size of memory block to be reallocated
	 * 
	 * @return    the address of reallocated memory block
	 * 
	* @see BigDataMemAllocator#reallocate(long, long)
	*/
	public MemChunkHolder<A> resizeChunk(MemChunkHolder<A> mholder, long size);
	
	public MemBufferHolder<A> resizeBuffer(MemBufferHolder<A> mholder, long size);

}
