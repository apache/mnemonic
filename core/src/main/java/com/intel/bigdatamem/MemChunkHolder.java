package com.intel.bigdatamem;

/**
 * holder for a memory chunk.
 * 
 */
public class MemChunkHolder<A extends CommonAllocator<A>> extends MemHolder<A, Long, MemChunkHolder<A>> {

    protected long m_size;

    /**
     * Constructor: initialize with a memory chunk.
     * 
     * @param ar
     *            specify an Allocator for this holder
     *            
     * @param mres
     *            specify a chunk to be holden
     *            
     * @param size
     *            specify the size of this memory chunk
     */
    public MemChunkHolder(A ar, Long mres, long size) {
	super(mres, ar);
	m_size = size;
    }
	
    /**
     * get the size of its held memory chunk
     * 
     * @return the size
     */
    @Override
    public long getSize() {
	return m_size;
    }

    /**
     * resize its held chunk
     *
     * @param size
     *          specify the new size for its held chunk
     */
    @Override
    public MemChunkHolder<A> resize(long size) {
	return m_allocator.resizeChunk(this, size);
    }

    /**
     * register its held chunk for auto-reclaim
     *
     */
    @Override
    public void registerAutoReclaim() {
	m_allocator.registerChunkAutoReclaim(this);
    }

}
