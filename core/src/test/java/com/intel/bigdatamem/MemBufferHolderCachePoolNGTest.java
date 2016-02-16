package com.intel.bigdatamem;

import static org.testng.Assert.*;
import org.testng.annotations.Test;

import com.intel.bigdatamem.BigDataMemAllocator;
import com.intel.bigdatamem.CachePool;
import com.intel.bigdatamem.ContainerOverflowException;
import com.intel.bigdatamem.EvictFilter;
import com.intel.bigdatamem.DropEvent;
import com.intel.bigdatamem.MemBufferHolder;
import com.intel.bigdatamem.MemBufferHolderCachePool;
import com.intel.bigdatamem.MemClustering;
import com.intel.bigdatamem.Utils;

import java.util.Random;

/**
 * test the functionalities of MemBufferHolderCachePool class
 * 
 * @author Wang, Gang(Gary) {@literal <gang1.wang@intel.com>}
 */
public class MemBufferHolderCachePoolNGTest {

	/**
	 * test to aggressively allow any MemBufferHolder objects in pool to be able
	 * to drop at will that prevents this cache pool from overflowing.
	 */
	@Test
	public void testMemBufferHolderCachePoolWithoutOverflow() {

		MemBufferHolderCachePool<Integer> mbhcpool = new MemBufferHolderCachePool<Integer>(
				1024 * 1024 * 10);
		Random randomGenerator = new Random();
		MemClustering.NodeConfig<?> ncs[] = new MemClustering.NodeConfig<?>[] { 
			new MemClustering.NodeConfig<BigDataMemAllocator>(
                                new BigDataMemAllocator(Utils.getVolatileMemoryAllocatorService("vmem"), 1024 * 1024 * 200, ".", true)
						.disableActiveGC(),
				MemClustering.PerformanceLevel.FAST),
		// new MemClustering.NodeConfig(new BigMemAllocator(1024*1024*20, ".",
		// true).disableActiveGC(), MemClustering.PerformanceLevel.NORMAL),
		// new MemClustering.NodeConfig(new BigMemAllocator(1024*1024*20, ".",
		// true).disableActiveGC(), MemClustering.PerformanceLevel.SLOW),
		};
		MemClustering mclst = new MemClustering(ncs);
		MemBufferHolder<?> mbh;

		DropEvent<Integer, MemBufferHolder<?>> dropevt = 
				new DropEvent<Integer, MemBufferHolder<?>>() {
			@Override
			public void drop(CachePool<Integer, MemBufferHolder<?>> pool,
					Integer k, MemBufferHolder<?> v) {
				System.out.println(String.format("dropping idx: %d", k));
				v.destroy();
			}
		};

		EvictFilter<Integer, MemBufferHolder<?>> dfilter = new EvictFilter<Integer, MemBufferHolder<?>>() {
			@Override
			public boolean validate(CachePool<Integer, MemBufferHolder<?>> pool,
					Integer k, MemBufferHolder<?> v) {
				System.out.println(String.format("validating idx: %d", k));
				return true;
			}
		};

		for (int idx = 1; idx <= 100; ++idx) {
			int size = randomGenerator.nextInt(1024 * 1024) + 1024 * 1024;
			mbh = mclst.createBuffer(size);
			System.out.println(String.format(
					"\nallocating idx: %d - size: %d, FreeCap: %d", idx, size,
					mbhcpool.freeCapacity()));
			assertNotNull(mbh);
			assertNotNull(mbh.get());
			for (int i = 0; i < size; i++) {
				mbh.get().put((byte) randomGenerator.nextInt(255));
			}

			mbh.get().flip();

			assertEquals(size, mbh.get().capacity());

			mbhcpool.put(idx, mbh, dropevt, dfilter);

		}
	}

	/**
	 * test to overflow a cache pool of MemBufferHolder objects that is caused
	 * by preventing any objected in pool from dropping.
	 */
	@Test(expectedExceptions = ContainerOverflowException.class)
	public void testMemBufferHolderCachePoolWithOverflow() {

		MemBufferHolderCachePool<Integer> mbhcpool = new MemBufferHolderCachePool<Integer>(
				1024 * 1024 * 10);
		Random randomGenerator = new Random();
		MemClustering.NodeConfig<?> ncs[] = new MemClustering.NodeConfig<?>[] { 
			new MemClustering.NodeConfig<BigDataMemAllocator>(
                                new BigDataMemAllocator(Utils.getVolatileMemoryAllocatorService("vmem"), 1024 * 1024 * 200, ".", true)
						.disableActiveGC(),
				MemClustering.PerformanceLevel.FAST),
		// new MemClustering.NodeConfig(new BigMemAllocator(1024*1024*20, ".",
		// true).disableActiveGC(), MemClustering.PerformanceLevel.NORMAL),
		// new MemClustering.NodeConfig(new BigMemAllocator(1024*1024*20, ".",
		// true).disableActiveGC(), MemClustering.PerformanceLevel.SLOW),
		};
		MemClustering mclst = new MemClustering(ncs);
		MemBufferHolder<?> mbh;

		DropEvent<Integer, MemBufferHolder<?>> dropevt = new DropEvent<Integer, MemBufferHolder<?>>() {
			@Override
			public void drop(CachePool<Integer, MemBufferHolder<?>> pool,
					Integer k, MemBufferHolder<?> v) {
				System.out.println(String.format("dropping idx: %d", k));
				v.destroy();
			}
		};

		EvictFilter<Integer, MemBufferHolder<?>> dfilter = new EvictFilter<Integer, MemBufferHolder<?>>() {
			@Override
			public boolean validate(CachePool<Integer, MemBufferHolder<?>> pool,
					Integer k, MemBufferHolder<?> v) {
				System.out.println(String.format("validating idx: %d", k));
				return false;
			}
		};

		for (int idx = 1; idx <= 100; ++idx) {
			int size = randomGenerator.nextInt(1024 * 1024) + 1024 * 1024;
			mbh = mclst.createBuffer(size);
			System.out.println(String.format(
					"\nallocating idx: %d - size: %d, FreeCap: %d", idx, size,
					mbhcpool.freeCapacity()));
			assertNotNull(mbh);
			assertNotNull(mbh.get());
			for (int i = 0; i < size; i++) {
				mbh.get().put((byte) randomGenerator.nextInt(255));
			}

			mbh.get().flip();

			assertEquals(size, mbh.get().capacity());

			mbhcpool.put(idx, mbh, dropevt, dfilter);

		}
	}

}
