package com.intel.bigdatamem;

import java.nio.ByteBuffer;

import org.flowcomputing.commons.resgc.*;
import org.flowcomputing.commons.primitives.*;
import com.intel.mnemonic.service.allocatorservice.NonVolatileMemoryAllocatorService;

/**
 * manage a big native persistent memory pool through libvmem.so provied by Intel nvml library.
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
	 * Constructor, it initialize and allocate a memory pool from specified uri
	 * location with specified capacity. usually, the uri points to a mounted
	 * non-volatile memory device or a location of file system.
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
	 * enable active garbage collection. the GC will be forced to perform when
	 * there is no more space to allocate.
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
	 */
	@Override
	public void close() {
		forceGC();
		super.close();
		m_nvmasvc.close(m_nid);
	}

	/**
	 * force to synchronize uncommitted data to backed memory pool
	 * (placeholder).
	 */
	@Override
	public void sync() {
	}

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
	 * @return the address of reallocated memory block from native memory pool
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
	 * create a MemChunkHolder object along with a memory chunk that is
	 * allocated from backed native memory pool.
	 * 
	 * @param size
	 *            specify the size of memory chunk
	 * 
	 * @return a created MemChunkHolder object
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
	 * create a MemBufferHolder object along with a ByteBuffer which is backed
	 * with a buffer allocated from backed native memory pool.
	 * 
	 * @param size
	 *            specify the size of backed memory buffer
	 * 
	 * @return a created MemBufferHolder object
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

	@Override
	public long getBufferHandler(MemBufferHolder<BigDataPMemAllocator> mbuf) {
		return getPortableAddress(m_nvmasvc.getByteBufferHandler(m_nid, mbuf.get()));
	}

	@Override
	public long getChunkHandler(MemChunkHolder<BigDataPMemAllocator> mchunk) {
		return getPortableAddress(mchunk.get());
	}

	public boolean supportPersistKey() {
		return true;
	}

	public void beginTransaction() {
		throw new UnsupportedOperationException("Transaction Unsupported.");
	}

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
	 * return the number of available handler keys to use.
	 * 
	 * @return the number of handler keys
	 * 
	 */
	public long handlerCapacity() {
		return m_nvmasvc.handlerCapacity(m_nid);
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
	
	@Override
	public long getPortableAddress(long addr) {
		return addr - b_addr;
	}

	@Override
	public long getEffectiveAddress(long addr) {
		return addr + b_addr;
	}

	@Override
	public long getBaseAddress() {
		return b_addr;
	}
	
	@Override
	public long setBaseAddress(long addr) {
		return b_addr = addr;
	}

}
