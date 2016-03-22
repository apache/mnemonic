package com.intel.bigdatamem;

import java.nio.ByteBuffer;

import org.flowcomputing.commons.resgc.*;
import org.flowcomputing.commons.primitives.*;
import com.intel.mnemonic.service.allocatorservice.NonVolatileMemoryAllocatorService;

/**
 * manage a big native persistent memory pool through libpmalloc.so provided by pmalloc project.
 * 
 *
 */
public class BigDataPMemAllocator extends CommonPersistAllocator<BigDataPMemAllocator> implements PMAddressTranslator{

    private boolean m_activegc = true;
    private long m_gctimeout = 100;
    private long m_nid = -1;
    private long b_addr = 0;
    private NonVolatileMemoryAllocatorService m_nvmasvc = null;

    /**
     * Constructor, it initializes and allocate a memory pool from specified uri
     * location with specified capacity and an allocator service instance. 
     * usually, the uri points to a mounted
     * non-volatile memory device or a location of file system.
     * 
     * @param nvmasvc
     *            the non-volatile memory allocation service instance
     *
     * @param capacity
     *            the capacity of memory pool
     * 
     * @param uri
     *            the location of memory pool will be created
     * 
     * @param isnew
     *            a place holder, always specify it as true
     */
    public BigDataPMemAllocator(NonVolatileMemoryAllocatorService nvmasvc, long capacity, String uri, boolean isnew) {
	assert null != nvmasvc : "NonVolatileMemoryAllocatorService object is null";
	if (capacity <= 0) {
	    throw new IllegalArgumentException("BigDataPMemAllocator cannot be initialized with capacity <= 0.");
	}

	m_nvmasvc = nvmasvc;
             
	m_nid = m_nvmasvc.init(capacity, uri, isnew);
	b_addr = m_nvmasvc.getBaseAddress(m_nid);
		
	/**
	 * create a resource collector to release specified chunk that backed by
	 * underlying big memory pool.
	 */
	m_chunkcollector = new ResCollector<MemChunkHolder<BigDataPMemAllocator>, Long>(new ResReclaim<Long>() {
		@Override
		public void reclaim(Long mres) {
		    // System.out.println(String.format("Reclaim: %X", mres));
		    boolean cb_reclaimed = false;
		    if (null != m_chunkreclaimer) {
			cb_reclaimed = m_chunkreclaimer.reclaim(mres, null);
		    }
		    if (!cb_reclaimed) {
			m_nvmasvc.free(m_nid, mres);
			mres = null;
		    }
		}
	    });

	/**
	 * create a resource collector to release specified bytebuffer that backed
	 * by underlying big memory pool.
	 */
	m_bufcollector = new ResCollector<MemBufferHolder<BigDataPMemAllocator>, ByteBuffer>(
											     new ResReclaim<ByteBuffer>() {
												 @Override
												 public void reclaim(ByteBuffer mres) {
												     boolean cb_reclaimed = false;
												     if (null != m_bufferreclaimer) {
													 cb_reclaimed = m_bufferreclaimer.reclaim(mres, Long.valueOf(mres.capacity()));
												     }
												     if (!cb_reclaimed) {
													 m_nvmasvc.destroyByteBuffer(m_nid, mres);
													 mres = null;
												     }
												 }
											     });
    }

    /**
     * enable active garbage collection. the GC will be forced to collect garbages when
     * there is no more space for current allocation request.
     * 
     * @param timeout
     *            the timeout is used to yield for GC performing
     */
    @Override
    public BigDataPMemAllocator enableActiveGC(long timeout) {
	m_activegc = true;
	m_gctimeout = timeout;
	return this;
    }

    /**
     * disable active garbage collection.
     * 
     */
    @Override
    public BigDataPMemAllocator disableActiveGC() {
	m_activegc = false;
	return this;
    }

    /**
     * Release the memory pool and close it.
     *
     */
    @Override
    public void close() {
	forceGC();
	super.close();
	m_nvmasvc.close(m_nid);
    }

    /**
     * force to synchronize uncommitted data to backed memory pool
     * (this is a placeholder).
     *
     */
    @Override
    public void sync() {
    }

    /**
     * re-size a specified chunk on its backed memory pool.
     * 
     * @param mholder
     *            the holder of memory chunk. it can be
     *            null.
     * 
     * @param size
     *            specify a new size of memory chunk
     * 
     * @return the resized memory chunk handler
     */
    @Override
    public MemChunkHolder<BigDataPMemAllocator> resizeChunk(MemChunkHolder<BigDataPMemAllocator> mholder, long size){
	MemChunkHolder<BigDataPMemAllocator> ret = null;
	boolean ac = null != mholder.getRefId();
	if (size > 0) {
	    Long addr = m_nvmasvc.reallocate(m_nid, mholder.get(), size, true);
	    if (0 == addr && m_activegc) {
		forceGC();
		addr = m_nvmasvc.reallocate(m_nid, mholder.get(), size, true);
	    }
	    if (0 != addr) {
		mholder.clear();
		mholder.destroy();
		ret = new MemChunkHolder<BigDataPMemAllocator>(this, addr, size);
		if (ac) {
		    m_chunkcollector.register(ret);
		}
	    }
	}
	return ret;
    }
	
    /**
     * resize a specified buffer on its backed memory pool.
     *
     * @param mholder
     *            the holder of memory buffer. it can be
     *            null.
     * 
     * @param size
     *            specify a new size of memory chunk
     * 
     * @return the resized memory buffer handler
     *
     */
    @Override
    public MemBufferHolder<BigDataPMemAllocator> resizeBuffer(MemBufferHolder<BigDataPMemAllocator> mholder, long size) {
	MemBufferHolder<BigDataPMemAllocator> ret = null;
	boolean ac = null != mholder.getRefId();
	if (size > 0) {
	    int bufpos = mholder.get().position();
	    int buflimit = mholder.get().limit();
	    ByteBuffer buf = m_nvmasvc.resizeByteBuffer(m_nid, mholder.get(), size);
	    if (null == buf && m_activegc) {
		forceGC();
		buf = m_nvmasvc.resizeByteBuffer(m_nid, mholder.get(), size);
	    }
	    if (null != buf) {
		mholder.clear();
		mholder.destroy();
		buf.position(bufpos <= size ? bufpos : 0);
		buf.limit(buflimit <= size ? buflimit : (int)size);
		ret = new MemBufferHolder<BigDataPMemAllocator>(this, buf);
		if (ac) {
		    m_bufcollector.register(ret);
		}
	    }
	}
	return ret;
    }

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
    @Override
    public MemChunkHolder<BigDataPMemAllocator> createChunk(long size, boolean autoreclaim) {
	MemChunkHolder<BigDataPMemAllocator> ret = null;
	Long addr = m_nvmasvc.allocate(m_nid, size, true);
	if ((null == addr || 0 == addr) && m_activegc) {
	    forceGC();
	    addr = m_nvmasvc.allocate(m_nid, size, true);
	}
	if (null != addr && 0 != addr) {
	    ret = new MemChunkHolder<BigDataPMemAllocator>(this, addr, size);
	    ret.setCollector(m_chunkcollector);
	    if (autoreclaim) {
		m_chunkcollector.register(ret);
	    }
	}
	return ret;
    }

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
    @Override
    public MemBufferHolder<BigDataPMemAllocator> createBuffer(long size, boolean autoreclaim) {
	MemBufferHolder<BigDataPMemAllocator> ret = null;
	ByteBuffer bb = m_nvmasvc.createByteBuffer(m_nid, size);
	if (null == bb && m_activegc) {
	    forceGC();
	    bb = m_nvmasvc.createByteBuffer(m_nid, size);
	}
	if (null != bb) {
	    ret = new MemBufferHolder<BigDataPMemAllocator>(this, bb);
	    ret.setCollector(m_bufcollector);
	    if (autoreclaim) {
		m_bufcollector.register(ret);
	    }
	}
	return ret;
    }

    /**
     * retrieve a memory buffer from its backed memory allocator.
     * 
     * @param phandler
     *            specify the handler of memory buffer to retrieve
     *
     * @param autoreclaim
     *            specify whether this retrieved memory buffer can be reclaimed automatically or not
     * 
     * @return a holder contains the retrieved memory buffer
     */
    @Override
    public MemBufferHolder<BigDataPMemAllocator> retrieveBuffer(long phandler, boolean autoreclaim) {
	MemBufferHolder<BigDataPMemAllocator> ret = null;
	ByteBuffer bb = m_nvmasvc.retrieveByteBuffer(m_nid, getEffectiveAddress(phandler));
	if (null != bb) {
	    ret = new MemBufferHolder<BigDataPMemAllocator>(this, bb);
	    if (autoreclaim) {
		m_bufcollector.register(ret);
	    }
	}
	return ret;
    }

    /**
     * retrieve a memory chunk from its backed memory allocator.
     * 
     * @param phandler
     *            specify the handler of memory chunk to retrieve
     *
     * @param autoreclaim
     *            specify whether this retrieved memory chunk can be reclaimed automatically or not
     * 
     * @return a holder contains the retrieved memory chunk
     */
    @Override
    public MemChunkHolder<BigDataPMemAllocator>  retrieveChunk(long phandler, boolean autoreclaim) {
	MemChunkHolder<BigDataPMemAllocator> ret = null;
	long eaddr = getEffectiveAddress(phandler);
	long sz = m_nvmasvc.retrieveSize(m_nid, eaddr);
	if (sz > 0L) {
	    ret = new MemChunkHolder<BigDataPMemAllocator>(this, eaddr, sz);
	    if (autoreclaim) {
		m_chunkcollector.register(ret);
	    }
	}
	return ret;
    }

    /**
     * get the handler from a memory buffer holder.
     * 
     * @param mbuf
     *            specify the memory buffer holder
     *
     * @return a handler that could be used to retrieve its memory buffer
     */
    @Override
    public long getBufferHandler(MemBufferHolder<BigDataPMemAllocator> mbuf) {
	return getPortableAddress(m_nvmasvc.getByteBufferHandler(m_nid, mbuf.get()));
    }

    /**
     * get the handler from a memory chunk holder.
     * 
     * @param mchunk
     *            specify the memory chunk holder
     *
     * @return a handler that could be used to retrieve its memory chunk
     */
    @Override
    public long getChunkHandler(MemChunkHolder<BigDataPMemAllocator> mchunk) {
	return getPortableAddress(mchunk.get());
    }

    /**
     * determine whether this allocator supports to store non-volatile handler or not.
     * (it is a placeholder)
     *
     * @return true if there is
     */
    @Override
    public boolean hasNonVolatileHandlerStore() {
	return true;
    }

    /**
     * start a application level transaction on this allocator.
     * (it is a place holder)
     *
     */
    @Override
    public void beginTransaction() {
	throw new UnsupportedOperationException("Transaction Unsupported.");
    }

    /**
     * end a application level transaction on this allocator.
     * (it is a place holder)
     *
     */
    @Override
    public void endTransaction() {
	throw new UnsupportedOperationException("Transaction Unsupported.");
    }

    /**
     * set a handler on key.
     * 
     * @param key
     *            the key to set its value
     *            
     * @param handler
     *            the handler 
     */
    public void setHandler(long key, long handler) {
	m_nvmasvc.setHandler(m_nid, key, handler);
    }

    /**
     * get a handler value.
     * 
     * @param key
     *            the key to set its value
     *            
     * @return the value of handler
     */
    public long getHandler(long key) {
	return m_nvmasvc.getHandler(m_nid, key);
    }
	
    /**
     * return the capacity of non-volatile handler store.
     * 
     * @return the capacity of handler store
     * 
     */
    public long handlerCapacity() {
	return m_nvmasvc.handlerCapacity(m_nid);
    }
	
    /**
     * force to perform GC that is used to re-claim garbages objects 
     * as well as memory resources managed by this allocator.
     *
     */
    private void forceGC() {
	System.gc();
	try {
	    Thread.sleep(m_gctimeout);
	} catch (Exception ex) {
	}
    }

    /**
     * calculate the portable address
     *
     * @param addr 
     *           the address to be calculated
     *
     * @return the portable address
     */
    @Override
    public long getPortableAddress(long addr) {
	return addr - b_addr;
    }

    /**
     * calculate the effective address
     *
     * @param addr 
     *           the address to be calculated
     *
     * @return the effective address
     */
    @Override
    public long getEffectiveAddress(long addr) {
	return addr + b_addr;
    }

    /**
     * get the base address
     *
     * @return the base address
     */
    @Override
    public long getBaseAddress() {
	return b_addr;
    }

    /**
     * set the base address for calculation
     *
     * @param addr 
     *           the base address
     *
     */
    @Override
    public long setBaseAddress(long addr) {
	return b_addr = addr;
    }

}
