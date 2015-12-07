package com.intel.bigdatamem;

import java.nio.ByteBuffer;

/**
 * hold a ByteBuffer object.
 * 
 * @author Wang, Gang {@literal <gang1.wang@intel.com>}
 *
 */
public class MemBufferHolder<A extends CommonAllocator<A>> extends MemHolder<A, ByteBuffer, MemBufferHolder<A>> {

	
	/**
	 * instantialise a ByteBuffer object.
	 * 
	 * @param ar
	 *            specify an Allocator for this holder
	 * 
	 * @param bb
	 *            specify the ByteBuffer object to be holden
	 */
	public MemBufferHolder(A ar, ByteBuffer bb) {
		super(bb, ar);
	}

	/**
	 * @see MemHolder#getSize()
	 */
	@Override
	public long getSize() {
		return m_mres.capacity();
	}
	
	/**
	 * reallocate its managed buffer.
	 * 
	 * @param size
	 *            specify a new size of memory chunk
	 * 
	 * @return true if success
	 */
	@Override
	public MemBufferHolder<A> resize(long size) {
		return m_allocator.resizeBuffer(this, size);
	}

	@Override
	public void registerAutoReclaim() {
		m_allocator.registerBufferAutoReclaim(this);
	}

}
