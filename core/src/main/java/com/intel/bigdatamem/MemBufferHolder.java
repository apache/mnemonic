package com.intel.bigdatamem;

import java.nio.ByteBuffer;

/**
 * holder for a ByteBuffer instance.
 * 
 */
public class MemBufferHolder<A extends CommonAllocator<A>> extends MemHolder<A, ByteBuffer, MemBufferHolder<A>> {

	
    /**
     * Constructor: initialize with a bytebuffer.
     * 
     * @param ar
     *            specify an Allocator for this holder
     *            
     * @param mres
     *            specify a chunk to be holden
     *            
     */
    public MemBufferHolder(A ar, ByteBuffer mres) {
	super(mres, ar);
    }

    /**
     * get the size of its held bytebuffer
     * 
     * @return the size
     */
    @Override
    public long getSize() {
	return m_mres.capacity();
    }
	
    /**
     * resize its held buffer
     *
     * @param size
     *          specify the new size for its held buffer 
     */
    @Override
    public MemBufferHolder<A> resize(long size) {
	return m_allocator.resizeBuffer(this, size);
    }

    /**
     * register its held buffer for auto-reclaim
     *
     */
    @Override
    public void registerAutoReclaim() {
	m_allocator.registerBufferAutoReclaim(this);
    }

}
