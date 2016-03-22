package com.intel.bigdatamem;

import java.nio.ByteBuffer;

import org.flowcomputing.commons.resgc.*;
import org.flowcomputing.commons.primitives.*;
import com.intel.mnemonic.service.allocatorservice.VolatileMemoryAllocatorService;

/**
 * manage a big native memory pool through libvmem.so that is provied by Intel nvml library.
 * 
 *
 */
public class BigDataMemAllocator extends CommonAllocator<BigDataMemAllocator> {

    private boolean m_activegc = true;
    private long m_gctimeout = 100;
    private long m_nid = -1;
    private VolatileMemoryAllocatorService m_vmasvc = null;

    /**
     * Constructor, it initializes and allocate a memory pool from specified uri
     * location with specified capacity and an allocator service instance. 
     * usually, the uri points to a mounted
     * memory device or a location of file system.
     * 
     * @param vmasvc
     *            the volatile memory allocation service instance
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
    public BigDataMemAllocator(VolatileMemoryAllocatorService vmasvc, long capacity, String uri, boolean isnew) {
	assert null != vmasvc : "VolatileMemoryAllocatorService object is null";
	if (capacity <= 0) {
	    throw new IllegalArgumentException("BigDataMemAllocator cannot be initialized with capacity <= 0.");
	}
                
	m_vmasvc = vmasvc;
	m_nid = m_vmasvc.init(capacity, uri, isnew);
		
	/**
	 * create a resource collector to release specified bytebuffer that backed
	 * by underlying big memory pool.
	 */
	m_bufcollector = new ResCollector<MemBufferHolder<BigDataMemAllocator>, ByteBuffer>(
											    new ResReclaim<ByteBuffer>() {
												@Override
												public void reclaim(ByteBuffer mres) {
												    boolean cb_reclaimed = false;
												    if (null != m_bufferreclaimer) {
													cb_reclaimed = m_bufferreclaimer.reclaim(mres, Long.valueOf(mres.capacity()));
												    }
												    if (!cb_reclaimed) {
													m_vmasvc.destroyByteBuffer(m_nid, mres);
													mres = null;
												    }
												}
											    });

	/**
	 * create a resource collector to release specified chunk that backed by
	 * underlying big memory pool.
	 */
	m_chunkcollector = new ResCollector<MemChunkHolder<BigDataMemAllocator>, Long>(
										       new ResReclaim<Long>() {
											   @Override
											   public void reclaim(Long mres) {
											       // System.out.println(String.format("Reclaim: %X", mres));
											       boolean cb_reclaimed = false;
											       if (null != m_chunkreclaimer) {
												   cb_reclaimed = m_chunkreclaimer.reclaim(mres, null);
											       }
											       if (!cb_reclaimed) {
												   m_vmasvc.free(m_nid, mres);
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
     *
     * @return this allocator
     */
    @Override
    public BigDataMemAllocator enableActiveGC(long timeout) {
	m_activegc = true;
	m_gctimeout = timeout;
	return this;
    }

    /**
     * disable active garbage collection.
     *
     * @return this allocator 
     */
    @Override
    public BigDataMemAllocator disableActiveGC() {
	m_activegc = false;
	return this;
    }

    /**
     * release the memory pool and close it.
     *
     */
    @Override
    public void close() {
	super.close();
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
    public MemChunkHolder<BigDataMemAllocator> resizeChunk(MemChunkHolder<BigDataMemAllocator> mholder, long size){
	MemChunkHolder<BigDataMemAllocator> ret = null;
	boolean ac = null != mholder.getRefId();
	if (size > 0) {
	    Long addr = m_vmasvc.reallocate(m_nid, mholder.get(), size, true);
	    if (0 == addr && m_activegc) {
		forceGC();
		addr = m_vmasvc.reallocate(m_nid, mholder.get(), size, true);
	    }
	    if (0 != addr) {
		mholder.clear();
		mholder.destroy();
		ret = new MemChunkHolder<BigDataMemAllocator>(this, addr, size);
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
    public MemBufferHolder<BigDataMemAllocator> resizeBuffer(MemBufferHolder<BigDataMemAllocator> mholder, long size) {
	MemBufferHolder<BigDataMemAllocator> ret = null;
	boolean ac = null != mholder.getRefId();
	if (size > 0) {
	    int bufpos = mholder.get().position();
	    int buflimit = mholder.get().limit();
	    ByteBuffer buf = m_vmasvc.resizeByteBuffer(m_nid, mholder.get(), size);
	    if (null == buf && m_activegc) {
		forceGC();
		buf = m_vmasvc.resizeByteBuffer(m_nid, mholder.get(), size);
	    }
	    if (null != buf) {
		mholder.clear();
		mholder.destroy();
		buf.position(bufpos <= size ? bufpos : 0);
		buf.limit(buflimit <= size ? buflimit : (int)size);
		ret = new MemBufferHolder<BigDataMemAllocator>(this, buf);
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
    public MemChunkHolder<BigDataMemAllocator> createChunk(long size, boolean autoreclaim) {
	MemChunkHolder<BigDataMemAllocator> ret = null;
	Long addr = m_vmasvc.allocate(m_nid, size, true);
	if (0 == addr && m_activegc) {
	    forceGC();
	    addr = m_vmasvc.allocate(m_nid, size, true);
	}
	if (0 != addr) {
	    ret = new MemChunkHolder<BigDataMemAllocator>(this, addr, size);
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
    public MemBufferHolder<BigDataMemAllocator> createBuffer(long size, boolean autoreclaim) {
	MemBufferHolder<BigDataMemAllocator> ret = null;
	ByteBuffer bb = m_vmasvc.createByteBuffer(m_nid, size);
	if (null == bb && m_activegc) {
	    forceGC();
	    bb = m_vmasvc.createByteBuffer(m_nid, size);
	}
	if (null != bb) {
	    ret = new MemBufferHolder<BigDataMemAllocator>(this, bb);
	    ret.setCollector(m_bufcollector);
	    if (autoreclaim) {
		m_bufcollector.register(ret);
	    }
	}
	return ret;
    }
	
    /**
     * force to perform GC that is used to release unused backed memory
     * resources.
     */
    private void forceGC() {
	System.gc();
	try {
	    Thread.sleep(m_gctimeout);
	} catch (Exception ex) {
	}
    }

}
