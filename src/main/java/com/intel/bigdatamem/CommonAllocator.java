
package com.intel.bigdatamem;

import java.nio.ByteBuffer;

import org.flowcomputing.commons.resgc.ResCollector;

/**
 * an abstract common class for memory allocator to provide common
 * functionalities.
 * 
 * @author Wang, Gang {@literal <gang1.wang@intel.com>}
 *
 */
public abstract class CommonAllocator<A extends CommonAllocator<A>> implements Allocator<A> {

	protected Reclaim<Long> m_chunkreclaimer = null;
	protected Reclaim<ByteBuffer> m_bufferreclaimer = null;
	
	protected ResCollector<MemChunkHolder<A>, Long> m_chunkcollector = null;
	protected ResCollector<MemBufferHolder<A>, ByteBuffer> m_bufcollector = null;
	/**
	 * set reclaimer for reclaiming memory buffer from this allocator.
	 * 
	 * @param reclaimer
	 *            specify a reclaimer to accept notification of reclaiming
	 */
	public void setBufferReclaimer(Reclaim<ByteBuffer> reclaimer) {
		m_bufferreclaimer = reclaimer;
	}

	/**
	 * set reclaimer for reclaiming memory chunk from this allocator.
	 * 
	 * @param reclaimer
	 *            specify a reclaimer to accept notification of reclaiming
	 */
	public void setChunkReclaimer(Reclaim<Long> reclaimer) {
		m_chunkreclaimer = reclaimer;
	}
	
	@Override
	public MemChunkHolder<A> createChunk(long size) {
		return createChunk(size, true);
	}

	@Override
	public MemBufferHolder<A> createBuffer(long size) {
		return createBuffer(size, true);
	}

	@Override
	public void registerChunkAutoReclaim(MemChunkHolder<A> mholder) {
		m_chunkcollector.register(mholder);
	}

	@Override
	public void registerBufferAutoReclaim(MemBufferHolder<A> mholder) {
		m_bufcollector.register(mholder);
	}

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
