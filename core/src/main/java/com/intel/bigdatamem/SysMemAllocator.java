
package com.intel.bigdatamem;

import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.flowcomputing.commons.resgc.*;

import sun.misc.Unsafe;
import sun.misc.Cleaner;

/**
 * manage a big system memory pool through Java NIO and Java Unsafe.
 * 
 * @author Wang, Gang {@literal <gang1.wang@intel.com>}
 *
 */
@SuppressWarnings("restriction")
public class SysMemAllocator extends CommonAllocator<SysMemAllocator> {

	private boolean m_activegc = true;
	private long m_gctimeout = 100;
	private Unsafe m_unsafe = null;
	private AtomicLong currentMemory = new AtomicLong(0L);
	private long maxStoreCapacity = 0L;
	private Map<Long, Long> m_chunksize = new ConcurrentHashMap<Long, Long>();


	/**
	 * Constructor, it initialize and allocate a memory pool from Java off-heap
	 * with specified capacity.
	 * 
	 * @param capacity
	 *            the capacity of memory pool
	 * 
	 * @param isnew
	 *            a place holder, always specify it as true
	 * 
	 * @throws Exception
	 *             fail to retrieve Unsafe object
	 * 
	 */
	public SysMemAllocator(long capacity, boolean isnew) throws Exception {
		m_unsafe = Utils.getUnsafe();
		maxStoreCapacity = capacity;
		/**
		 * create a resource collector to release specified bytebuffer that backed
		 * by Java off-heap.
		 */
		m_bufcollector = new ResCollector<MemBufferHolder<SysMemAllocator>, ByteBuffer>(
				new ResReclaim<ByteBuffer>() {
					@Override
					public synchronized void reclaim(ByteBuffer mres) {
						Long sz = Long.valueOf(mres.capacity());
						boolean cb_reclaimed = false;
						if (null != m_bufferreclaimer) {
							cb_reclaimed = m_bufferreclaimer.reclaim(mres, sz);
						}
						if (!cb_reclaimed) {
							try {
								Field cleanerField;
								cleanerField = mres.getClass().getDeclaredField(
										"cleaner");
								cleanerField.setAccessible(true);
								Cleaner cleaner = (Cleaner) cleanerField.get(mres);
								cleaner.clean();
							} catch (NoSuchFieldException | SecurityException
									| IllegalArgumentException | IllegalAccessException e) {
								e.printStackTrace();
							}
							mres = null;
						}
						currentMemory.addAndGet(-sz);
					}
				});

		/**
		 * create a resource collector to release specified chunk that backed by
		 * Java off-heap.
		 */
		m_chunkcollector = new ResCollector<MemChunkHolder<SysMemAllocator>, Long>(
				new ResReclaim<Long>() {
					@Override
					public synchronized void reclaim(Long mres) {
						// System.out.println(String.format("Reclaim: %X ...", mres));
						Long sz = m_chunksize.remove(mres);
						boolean cb_reclaimed = false;
						if (null != m_chunkreclaimer) {
							cb_reclaimed = m_chunkreclaimer.reclaim(mres, sz);
						}
						if (!cb_reclaimed) {
							m_unsafe.freeMemory(mres);
							mres = null;
						}
						if (null != sz) {
							currentMemory.addAndGet(-sz);
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
	public SysMemAllocator enableActiveGC(long timeout) {
		m_activegc = true;
		m_gctimeout = timeout;
		return this;
	}

	/**
	 * disable active garbage collection.
	 * 
	 */
	@Override
	public SysMemAllocator disableActiveGC() {
		m_activegc = false;
		return this;
	}

	/**
	 * Release the memory pool and close it.
	 */
	@Override
	public void close() {
		super.close();
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
	 * @return the address of reallocated memory block from Java off-heap
	 */
	@Override
	public MemChunkHolder<SysMemAllocator> resizeChunk(MemChunkHolder<SysMemAllocator> mholder, long size){
		MemChunkHolder<SysMemAllocator> ret = null;
		boolean ac = null != mholder.getRefId();
		if (size > 0) {
			if (currentMemory.get() + size > maxStoreCapacity) {
				if (m_activegc) {
					forceGC();
				}
			}
			if (currentMemory.get() + size <= maxStoreCapacity) {
				Long addr = m_unsafe.reallocateMemory(mholder.get(), size);
				if (0 != addr) {
					mholder.clear();
					mholder.destroy();
					ret = new MemChunkHolder<SysMemAllocator>(this, addr, size);
					if (ac) {
						m_chunkcollector.register(ret);
					}
				}
			}
		}
		return ret;
	}
	
	@Override
	public MemBufferHolder<SysMemAllocator> resizeBuffer(MemBufferHolder<SysMemAllocator> mholder, long size) {
		MemBufferHolder<SysMemAllocator> ret = null;
		boolean ac = null != mholder.getRefId();
		if (size > 0) {
			int bufpos = mholder.get().position();
			int buflimit = mholder.get().limit();
			if (currentMemory.get() + size > maxStoreCapacity) {
				if (m_activegc) {
					forceGC();
				}
			}
			if (currentMemory.get() + size <= maxStoreCapacity) {
				ByteBuffer buf = Utils.resizeByteBuffer(mholder.get(), size);
				if (null != buf) {
					mholder.clear();
					mholder.destroy();
					buf.position(bufpos <= size ? bufpos : 0);
					buf.limit(buflimit <= size ? buflimit : (int)size);
					ret = new MemBufferHolder<SysMemAllocator>(this, buf);
					if (ac) {
						m_bufcollector.register(ret);
					}
				}
			}
		}
		return ret;
	}

	/**
	 * create a MemChunkHolder object along with a memory chunk that is
	 * allocated from Java off-heap.
	 * 
	 * @param size
	 *            specify the size of memory chunk
	 * 
	 * @return a created MemChunkHolder object
	 */
	@Override
	public MemChunkHolder<SysMemAllocator> createChunk(long size, boolean autoreclaim) {
		MemChunkHolder<SysMemAllocator> ret = null;
		Long addr = null;
		if (currentMemory.get() + size > maxStoreCapacity) {
			if (m_activegc) {
				forceGC();
			}
		}
		if (currentMemory.get() + size <= maxStoreCapacity) {
			addr = m_unsafe.allocateMemory(size);
		}
		if (null != addr && 0 != addr) {
			ret = new MemChunkHolder<SysMemAllocator>(this, addr, size);
			ret.setCollector(m_chunkcollector);
			if (autoreclaim) {
				m_chunkcollector.register(ret);
			}
			m_chunksize.put(addr, size);
			currentMemory.getAndAdd(size);
		}
		return ret;
	}

	/**
	 * create a MemBufferHolder object along with a ByteBuffer which is backed
	 * with a buffer allocated from backed Java off-heap.
	 * 
	 * @param size
	 *            specify the size of backed memory buffer
	 * 
	 * @return a created MemBufferHolder object
	 */
	@Override
	public MemBufferHolder<SysMemAllocator> createBuffer(long size, boolean autoreclaim) {
		MemBufferHolder<SysMemAllocator> ret = null;
		ByteBuffer bb = null;
		if (currentMemory.get() + size > maxStoreCapacity) {
			if (m_activegc) {
				forceGC();
			}
		}
		if (currentMemory.get() + size <= maxStoreCapacity) {
			bb = ByteBuffer.allocateDirect((int) size);
		}
		if (null != bb) {
			ret = new MemBufferHolder<SysMemAllocator>(this, bb);
			ret.setCollector(m_bufcollector);
			if (autoreclaim) {
				m_bufcollector.register(ret);
			}
			currentMemory.getAndAdd(size);
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
