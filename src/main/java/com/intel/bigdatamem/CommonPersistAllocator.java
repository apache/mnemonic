package com.intel.bigdatamem;

/**
 * an abstract common class for persistent memory allocator to provide common
 * functionalities.
 * 
 * @author Wang, Gang {@literal <gang1.wang@intel.com>}
 *
 */
public abstract class CommonPersistAllocator<A extends CommonAllocator<A>> extends CommonAllocator<A> {

	public boolean supportTransaction() {
		return false;
	}
	
	public boolean isAtomOperation() {
		return false;
	}
	
	public boolean supportPersistKey() {
		return false;
	}

	public MemBufferHolder<A> retrieveBuffer(long phandler) {
		return retrieveBuffer(phandler, true);
	}

	public MemChunkHolder<A>  retrieveChunk(long phandler) {
		return retrieveChunk(phandler, true);
	}

	abstract public MemBufferHolder<A> retrieveBuffer(long phandler, boolean autoreclaim);

	abstract public MemChunkHolder<A>  retrieveChunk(long phandler, boolean autoreclaim);

	abstract public long getBufferHandler(MemBufferHolder<A> mbuf);

	abstract public long getChunkHandler(MemChunkHolder<A> mchunk);

	abstract public void beginTransaction();

	abstract public void endTransaction();

}
