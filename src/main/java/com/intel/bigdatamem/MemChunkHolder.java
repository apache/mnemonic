package com.intel.bigdatamem;

/**
 * hold a memory chunk.
 * 
 * @author Wang, Gang {@literal <gang1.wang@intel.com>}
 *
 */
public class MemChunkHolder<A extends CommonAllocator<A>> extends MemHolder<A, Long, MemChunkHolder<A>> {

	protected long m_size;

	/**
	 * instantiate a MemChunkHolder object.
	 * 
	 * @param ar
	 *            specify an Allocator for this holder
	 * 
	 * @param addr
	 *            specify the address of memory chunk to be holden
	 * 
	 * @param size
	 *            specify the size of memory chunk
	 */
	public MemChunkHolder(A ar, Long addr, long size) {
		super(addr, ar);
		m_size = size;
	}
	
	/**
	 * @see MemHolder#getSize()
	 */
	@Override
	public long getSize() {
		return m_size;
	}

	/**
	 * reallocate its holden memory chunk.
	 * 
	 * @param size
	 *            specify a new size of memory chunk
	 * 
	 * @return true if success
	 */
	@Override
	public MemChunkHolder<A> resize(long size) {
		return m_allocator.resizeChunk(this, size);
	}

	@Override
	public void registerAutoReclaim() {
		m_allocator.registerChunkAutoReclaim(this);
	}

}
