package com.intel.bigdatamem;

import java.nio.ByteBuffer;

import org.flowcomputing.commons.resgc.*;
import org.flowcomputing.commons.primitives.*;

/**
 * manage a big native persistent memory pool through libvmem.so provied by Intel nvml library.
 * 
 *
 */
public class BigDataPMemAllocator extends CommonPersistAllocator<BigDataPMemAllocator> implements PMAddressTranslator{
	static {
		try {
			NativeLibraryLoader.loadFromJar("bigdatapmem");
		} catch (Exception e) {
			System.exit(-1);
		}
	}

	private boolean m_activegc = true;
	private long m_gctimeout = 100;
	private long m_nid = -1;
	private long b_addr = 0;

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
	public BigDataPMemAllocator(long capacity, String uri, boolean isnew) {
		m_nid = ninit(capacity, uri, isnew);
		b_addr = ngetBaseAddress(m_nid);
		
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
							nfree(m_nid, mres);
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
								ndestroyByteBuffer(m_nid, mres);
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
	 * Initialize a memory pool through native interface backed by native
	 * library.
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
	protected native long ninit(long capacity, String uri, boolean isnew);

	/**
	 * Release the memory pool and close it.
	 */
	@Override
	public void close() {
		forceGC();
		super.close();
		nclose(m_nid);
	}

	/**
	 * close the memory pool through native interface.
	 * 
	 */
	protected native void nclose(long id);

	/**
	 * force to synchronize uncommitted data to backed memory pool
	 * (placeholder).
	 */
	@Override
	public void sync() {
	}

	/**
	 * force to synchronize uncommitted data to backed memory pool through
	 * native interface.
	 */
	protected native void nsync(long id);

	/**
	 * allocate specified size of memory block from backed memory pool.
	 * 
	 * @param id
	 *            the identifier of backed memory pool
	 * 
	 * @param size
	 *            specify size of memory block to be allocated
	 * 
	 * @return the address of allocated memory block from native memory pool
	 */
	protected native long nallocate(long id, long size, boolean initzero);

	/**
	 * reallocate a specified size of memory block from backed memory pool.
	 * 
	 * @param id
	 *            the identifier of backed memory pool
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
	protected native long nreallocate(long id, long address, long size, boolean initzero);

	/**
	 * free a memory block by specify its address into backed memory pool.
	 * 
	 * @param id
	 *            the identifier of backed memory pool
	 * 
	 * @param address
	 *            the address of allocated memory block.
	 */
	protected native void nfree(long id, long address);

	/**
	 * create a ByteBuffer object which backed buffer is coming from backed
	 * native memory pool.
	 * 
	 * @param id
	 *            the identifier of backed memory pool
	 * 
	 * @param size
	 *            the size of backed buffer that is managed by created
	 *            ByteBuffer object.
	 * 
	 * @return a created ByteBuffer object with a backed native memory block
	 */
	protected native ByteBuffer ncreateByteBuffer(long id, long size);
	
	/**
	 * retrieve a persistent ByteBuffer object which backed buffer is coming from backed
	 * native memory pool.
	 * 
	 * @param id
	 *            the identifier of backed memory pool
	 * 
	 * @param eaddr
	 *            the effective address of persistent
	 *            ByteBuffer object.
	 * 
	 * @return a created ByteBuffer object with a backed native memory block
	 */
	protected native ByteBuffer nretrieveByteBuffer(long id, long eaddr);

	protected native long nretrieveSize(long id, long eaddr);
	
	protected native long ngetByteBufferAddress(long id, ByteBuffer buf);
	/**
	 * resize a ByteBuffer object which backed buffer is coming from backed
	 * native memory pool.
	 * NOTE: the ByteBuffer object will be renewed and lost metadata e.g. position, mark and etc.
	 * 
	 * @param id
	 *            the identifier of backed memory pool
	 * 
	 * @param bytebuf
	 *            the specified ByteBuffer object to be destroyed
	 *            
	 * @param size
	 *            the new size of backed buffer that is managed by created
	 *            ByteBuffer object.
	 * 
	 * @return a created ByteBuffer object with a backed native memory block
	 */
	protected native ByteBuffer nresizeByteBuffer(long id, ByteBuffer bytebuf, long size);
	
	/**
	 * destroy a native memory block backed ByteBuffer object.
	 * 
	 * @param id
	 *            the identifier of backed memory pool
	 * 
	 * @param bytebuf
	 *            the specified ByteBuffer object to be destroyed
	 */
	protected native void ndestroyByteBuffer(long id, ByteBuffer bytebuf);
	
	/**
	 * set a persistent key value.
	 * 
	 * @param id
	 *            the identifier of backed memory pool
	 * 
	 * @param key
	 *            the key to set its value
	 *            
	 * @param value
	 *            the value 
	 */
	protected native void nsetPersistKey(long id, long key, long value);
	
	/**
	 * get a persistent key's value.
	 * 
	 * @param id
	 *            the identifier of backed memory pool
	 * 
	 * @param key
	 *            the key to set its value
	 *            
	 * @return the value of the specified key
	 */
	protected native long ngetPersistKey(long id, long key);
	
	/**
	 * return the number of available keys to use.
	 * 
	 * @param id
	 *            the identifier of backed memory pool
	 *            
	 * @return the number of keys
	 */
	protected native long npersistKeyCapacity(long id);
		
	/**
	 * return the base address of this persistent memory pool.
	 * 
	 * @param id
	 *            the identifier of backed memory pool
	 *            
	 * @return the base address of this pmem pool
	 */
	protected native long ngetBaseAddress(long id);

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
			Long addr = nreallocate(m_nid, mholder.get(), size, true);
			if (0 == addr && m_activegc) {
				forceGC();
				addr = nreallocate(m_nid, mholder.get(), size, true);
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
			ByteBuffer buf = nresizeByteBuffer(m_nid, mholder.get(), size);
			if (null == buf && m_activegc) {
				forceGC();
				buf = nresizeByteBuffer(m_nid, mholder.get(), size);
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
		Long addr = nallocate(m_nid, size, true);
		if ((null == addr || 0 == addr) && m_activegc) {
			forceGC();
			addr = nallocate(m_nid, size, true);
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
		ByteBuffer bb = ncreateByteBuffer(m_nid, size);
		if (null == bb && m_activegc) {
			forceGC();
			bb = ncreateByteBuffer(m_nid, size);
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
		ByteBuffer bb = nretrieveByteBuffer(m_nid, getEffectiveAddress(phandler));
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
		long sz = nretrieveSize(m_nid, eaddr);
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
		return getPortableAddress(ngetByteBufferAddress(m_nid, mbuf.get()));
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
	 * set a persistent key value.
	 * 
	 * @param key
	 *            the key to set its value
	 *            
	 * @param value
	 *            the value 
	 */
	public void setPersistKey(long key, long value) {		
		nsetPersistKey(m_nid, key, value);
	}

	/**
	 * get a persistent key's value.
	 * 
	 * @param key
	 *            the key to set its value
	 *            
	 * @return the value of the specified key
	 */
	public long getPersistKey(long key) {
		return ngetPersistKey(m_nid, key);
	}
	
	/**
	 * return the number of available keys to use.
	 * 
	 * @return the number of keys
	 * 
	 */
	public long persistKeyCapacity() {
		return npersistKeyCapacity(m_nid);
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
