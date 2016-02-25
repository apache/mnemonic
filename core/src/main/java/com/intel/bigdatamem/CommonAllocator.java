package com.intel.bigdatamem;

import java.nio.ByteBuffer;

import org.flowcomputing.commons.resgc.ResCollector;

/**
 * an abstract common class for memory allocator to provide common
 * functionalities.
 * 
 */
public abstract class CommonAllocator<A extends CommonAllocator<A>> implements Allocator<A> {

    protected Reclaim<Long> m_chunkreclaimer = null;
    protected Reclaim<ByteBuffer> m_bufferreclaimer = null;
	
    protected ResCollector<MemChunkHolder<A>, Long> m_chunkcollector = null;
    protected ResCollector<MemBufferHolder<A>, ByteBuffer> m_bufcollector = null;
    
    /**
     * set a reclaimer to reclaim memory buffer
     * 
     * @param reclaimer
     *            specify a reclaimer to accept reclaim request
     */
    public void setBufferReclaimer(Reclaim<ByteBuffer> reclaimer) {
	m_bufferreclaimer = reclaimer;
    }

    /**
     * set a reclaimer to reclaim memory chunk
     * 
     * @param reclaimer
     *            specify a reclaimer to accept reclaim request
     */
    public void setChunkReclaimer(Reclaim<Long> reclaimer) {
	m_chunkreclaimer = reclaimer;
    }
	
    /**
     * create a memory chunk that is managed by its holder.
     * 
     * @param size
     *            specify the size of memory chunk
     * 
     * @return a holder contains a memory chunk
     */
    @Override
    public MemChunkHolder<A> createChunk(long size) {
	return createChunk(size, true);
    }

    /**
     * create a memory buffer that is managed by its holder.
     * 
     * @param size
     *            specify the size of memory buffer
     * 
     * @return a holder contains a memory buffer
     */
    @Override
    public MemBufferHolder<A> createBuffer(long size) {
	return createBuffer(size, true);
    }

    /**
     * register a memory chunk for auto-reclaim
     *
     * @param mholder
     *           specify a chunk holder to register
     */
    @Override
    public void registerChunkAutoReclaim(MemChunkHolder<A> mholder) {
	m_chunkcollector.register(mholder);
    }

    /**
     * register a memory buffer for auto-reclaim
     *
     * @param mholder
     *           specify a buffer holder to register
     */
    @Override
    public void registerBufferAutoReclaim(MemBufferHolder<A> mholder) {
	m_bufcollector.register(mholder);
    }

    /**
     * close both of resource collectors for this allocator
     *
     */
    @Override
    public void close() {
	if (null != m_chunkcollector) {
	    m_chunkcollector.close();
	    m_chunkcollector = null;
	}
	if (null != m_bufcollector) {
	    m_bufcollector.close();
	    m_bufcollector = null;
	}
    }
	
}
