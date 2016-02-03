
package com.intel.bigdatamem;

import java.util.*;

/**
 * clustering different kind of memory storage and combine them as a larger
 * memory pool for allocation. it will notify client when the underlying memory
 * storage has been switched or down-grading to next level of memory storage.
 * 
 *
 */
public class MemClustering {

	/**
	 * an interface of event for performance level change.
	 *
	 */
	public interface PerformanceLevelChange {
		public void changed(PerformanceLevel prevlvl, PerformanceLevel tgtlvl);
	}

	/**
	 * an interface of event for CommonAllocator change.
	 *
	 */
	public interface AllocatorChange {
		public void changed(PerformanceLevel lvl, CommonAllocator<?> prevallocator,
				CommonAllocator<?> tgtallocator);
	}

	/**
	 * an interface to assist memory holder creating.
	 *
	 * @param <HOLDER>
	 *            the holder type of memory block
	 */
	private interface MemCreate<H extends MemHolder<? extends CommonAllocator<?>, ?, ?>> {
		public H create(CommonAllocator<?> bma, long size);
	}
	
	private MemCreate<MemChunkHolder<?>> m_memchunkcreate = new MemCreate<MemChunkHolder<?>>() {
		@Override
		public MemChunkHolder<?> create(CommonAllocator<?> bma, long size) {
			return bma.createChunk(size);
		}
	};
	
	private MemCreate<MemBufferHolder<?>> m_membuffercreate = new MemCreate<MemBufferHolder<?>>() {
		@Override
		public MemBufferHolder<?> create(CommonAllocator<?> bma, long size) {
			return bma.createBuffer(size);
		}
	};

	/**
	 * performance level categories.
	 *
	 */
	public static enum PerformanceLevel {
		FASTEST, FAST, NORMAL, SLOW, SLOWEST
	}

	/**
	 * configuration for each allocator node.
	 *
	 */
	public static class NodeConfig<A extends CommonAllocator<A>> {
		private A m_allocator;
		private PerformanceLevel m_level;

		/**
		 * initialize this instance with specified allocator and level.
		 * 
		 * @param a
		 *            specify an allocator for this node
		 * 
		 * @param l
		 *            specify a performance level
		 */
		public NodeConfig(A a, PerformanceLevel l) {
			m_allocator = a;
			m_level = l;
		}

		/**
		 * retrieve the allocator of this node.
		 * 
		 * @return allocator of this node
		 */
		public A getAllocator() {
			return m_allocator;
		}

		/**
		 * retrieve the performance level of this node.
		 * 
		 * @return level of this node
		 */
		public PerformanceLevel getPerformanceLevel() {
			return m_level;
		}
	}

	private PerformanceLevelChange m_bwlvlchange = null;
	private AllocatorChange m_allocatorChange = null;
	private PerformanceLevel m_prevbwlevel = null;
	private CommonAllocator<?> m_prevallocator = null;


	private Map<PerformanceLevel, List<CommonAllocator<?>>> m_info;

	/**
	 * initialize a memory clustering object.
	 * 
	 * @param ncs
	 *            specify a set of node with specified configuration
	 */
	public MemClustering(NodeConfig<? extends CommonAllocator<?>>[] ncs) {
		m_info = new EnumMap<PerformanceLevel, List<CommonAllocator<?>>>(
				PerformanceLevel.class);
		for (PerformanceLevel lvl : PerformanceLevel.values()) {
			m_info.put(lvl, new ArrayList<CommonAllocator<?>>());
		}

		for (NodeConfig<? extends CommonAllocator<?>> nc : ncs) {
			m_info.get(nc.getPerformanceLevel()).add(nc.getAllocator());
		}

	}

	/**
	 * set a callback of event for performance level change.
	 * 
	 * @param bwlvlchange
	 *            specify a callback object of performance change
	 */
	public void setPerformanceLevelChange(PerformanceLevelChange bwlvlchange) {
		m_bwlvlchange = bwlvlchange;
	}

	/**
	 * set a callback of event for allocator change.
	 * 
	 * @param allocatorChange
	 *            specify a callback object of allocator change
	 */
	public <A extends CommonAllocator<A>> void setAllocatorChange(AllocatorChange allocatorChange) {
		m_allocatorChange = allocatorChange;
	}

	/**
	 * create factory for new memory resource.
	 * 
	 * @param creator
	 *            specify a creator delegate for concrete holder creation
	 * 
	 * @param startlevel
	 *            specify a level that is the start level for memory resource
	 *            allocation
	 * 
	 * @param size
	 *            specify a size to request memory resource
	 * 
	 * @return a new created holder with memory resource
	 */
	protected <H extends MemHolder<? extends CommonAllocator<?>, ?, ?>> H
			create(MemCreate<H> creator, PerformanceLevel startlevel, long size) {
		H ret = null;
		boolean eligible = false;
		for (PerformanceLevel lvl : m_info.keySet()) {
			if (!eligible && startlevel == lvl) {
				eligible = true;
			}
			if (eligible) {
				int distance = 0;
				List<CommonAllocator<?>> bmas = m_info.get(lvl);
				for (CommonAllocator<?> bma : bmas) {
					ret = creator.create(bma, size);
					if (null == ret) {
						distance++;
					} else {
						if (null != m_bwlvlchange && m_prevbwlevel != lvl) {
							m_bwlvlchange.changed(m_prevbwlevel, lvl);
							m_prevbwlevel = lvl;
						}
						if (null != m_allocatorChange && m_prevallocator != bma) {
							m_allocatorChange.changed(lvl, m_prevallocator, bma);
							m_prevallocator = bma;
						}
						break;
					}
				}
				Collections.rotate(bmas, distance);
			}
			if (null != ret) {
				break;
			}
		}
		return ret;
	}

	/**
	 * create a holder of a memory chunk from one of node of memory clustering.
	 * 
	 * @param <A> the type of bound allocator 
	 * 
	 * @param size
	 *            specify the size of memory chunk
	 * 
	 * @return a new created memory chunk holder
	 * 
	 * @see #createChunk(PerformanceLevel, long)
	 */
	//@Override
	public MemChunkHolder<?> createChunk(long size) {
		return createChunk(PerformanceLevel.FASTEST, size);
	}

	/**
	 * create a holder of a memory chunk from one of node of memory clustering.
	 * 
	 * @param <A> the type of bound allocator 
	 * 
	 * @param startlevel
	 *            specify the performance level from which to search available
	 *            node.
	 * 
	 * @param size
	 *            specify the size of memory chunk
	 * 
	 * @return a new created memory chunk holder
	 */
	
	public MemChunkHolder<?> createChunk(PerformanceLevel startlevel, long size) {
		return create(m_memchunkcreate, startlevel, size);
	}

	/**
	 * create a holder of a memory buffer from one of node of memory clustering.
	 * 
	 * @param <A> the type of bound allocator 
	 * 
	 * @param size
	 *            specify the size of memory buffer
	 * 
	 * @return a new created memory buffer holder
	 * 
	 * @see #createByteBuffer(PerformanceLevel, long)
	 */
	//@Override
	public MemBufferHolder<?> createBuffer(long size) {
		return createBuffer(PerformanceLevel.FASTEST, size);
	}

	/**
	 * create a holder of a memory chunk from one of node of memory clustering.
	 * 
	 * @param <A> the type of bound allocator 
	 * 
	 * @param startlevel
	 *            specify the performance level from which to search available
	 *            node.
	 * 
	 * @param size
	 *            specify the size of memory buffer
	 * 
	 * @return a new created memory buffer holder
	 */
	public MemBufferHolder<?> createBuffer(PerformanceLevel startlevel,
			long size) {
		return create(m_membuffercreate, startlevel, size);
	}

//	@Override
//	public MemChunkHolder<A> resizeChunk(MemChunkHolder<A> mholder, long size) {
//		throw new UnsupportedOperationException();
//	}
//
//	@Override
//	public MemBufferHolder<A> resizeBuffer(MemBufferHolder<A> mholder, long size) {
//		throw new UnsupportedOperationException();
//	}

}
