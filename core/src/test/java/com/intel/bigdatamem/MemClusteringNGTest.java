
package com.intel.bigdatamem;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

import com.intel.bigdatamem.BigDataMemAllocator;
import com.intel.bigdatamem.CommonAllocator;
import com.intel.bigdatamem.MemBufferHolder;
import com.intel.bigdatamem.MemClustering;
import com.intel.bigdatamem.SysMemAllocator;
import com.intel.bigdatamem.Utils;

import java.util.Random;

/**
 * test the functionalities of MemClustering class
 * 
 * @author Wang, Gang(Gary) {@literal <gang1.wang@intel.com>}
 */
public class MemClusteringNGTest {

	/**
	 * test to fill up memory pool without reclaim unused memory blocks that
	 * will cause additional operations of allocation will be failed.
	 * 
	 * @throws Exception
	 */
	@Test(expectedExceptions = NullPointerException.class)
	public void testMemByteBufferWithoutActiveGC() throws Exception {
		Random randomGenerator = new Random();
		MemClustering.NodeConfig<?> ncs[] = new MemClustering.NodeConfig<?>[] {
				new MemClustering.NodeConfig<SysMemAllocator>(new SysMemAllocator(
						1024 * 1024 * 20, true).disableActiveGC(),
						MemClustering.PerformanceLevel.FASTEST),
                                new MemClustering.NodeConfig<BigDataMemAllocator>(new BigDataMemAllocator(Utils.getVolatileMemoryAllocatorService("vmem"),
						1024 * 1024 * 20, ".", true).disableActiveGC(),
						MemClustering.PerformanceLevel.FAST),
				// new MemClustering.NodeConfig(new
				// BigMemAllocator(1024*1024*20, ".", true).disableActiveGC(),
				// MemClustering.PerformanceLevel.NORMAL),
                                new MemClustering.NodeConfig<BigDataMemAllocator>(new BigDataMemAllocator(Utils.getVolatileMemoryAllocatorService("vmem"),
						1024 * 1024 * 20, ".", true).disableActiveGC(),
						MemClustering.PerformanceLevel.SLOW), };
		MemClustering mclst = new MemClustering(ncs);
		MemBufferHolder<?> mbh;
		for (int idx = 1; idx <= 200; ++idx) {
			int size = randomGenerator.nextInt(1024 * 1024) + 1024 * 1024;
			mbh = mclst.createBuffer(size);
			for (int i = 0; i < size; i++) {
				mbh.get().put((byte) randomGenerator.nextInt(255));
			}
			assertEquals(size, mbh.get().capacity());
		}
	}

	/**
	 * test to try to fill up memory pool and reclaim unused memory block as
	 * required.
	 */
	@Test
	public void testMemByteBufferWithActiveGC() {
		Random randomGenerator = new Random();
		MemClustering.NodeConfig<?> ncs[] = new MemClustering.NodeConfig<?>[] { new MemClustering.NodeConfig<BigDataMemAllocator>(
                                new BigDataMemAllocator(Utils.getVolatileMemoryAllocatorService("vmem"), 1024 * 1024 * 20, ".", true),
				MemClustering.PerformanceLevel.NORMAL),
		// new MemClustering.NodeConfig(new BigMemAllocator(1024*1024*20, ".",
		// true), MemClustering.PerformanceLevel.SLOW),
		};
		MemClustering mclst = new MemClustering(ncs);
		MemBufferHolder<?> mbh;
		for (int idx = 1; idx <= 200; ++idx) {
			int size = randomGenerator.nextInt(1024 * 1024) + 1024 * 1024;
			mbh = mclst.createBuffer(size);
			for (int i = 0; i < size; i++) {
				mbh.get().put((byte) randomGenerator.nextInt(255));
			}
			assertEquals(size, mbh.get().capacity());
		}
	}

	/**
	 * test to manually release memory resource once that is intended to be
	 * unused.
	 */
	@Test
	public void testMemByteBufferManualRelease() {
		Random randomGenerator = new Random();
		MemClustering.NodeConfig<?> ncs[] = new MemClustering.NodeConfig<?>[] {
				new MemClustering.NodeConfig<BigDataMemAllocator>(new BigDataMemAllocator(
                                                Utils.getVolatileMemoryAllocatorService("vmem"),
						1024 * 1024 * 20, ".", true).disableActiveGC(),
						MemClustering.PerformanceLevel.FAST),
				// new MemClustering.NodeConfig(new
				// BigMemAllocator(1024*1024*20, ".", true).disableActiveGC(),
				// MemClustering.PerformanceLevel.NORMAL),
				new MemClustering.NodeConfig<BigDataMemAllocator>(new BigDataMemAllocator(
                                                Utils.getVolatileMemoryAllocatorService("vmem"),
						1024 * 1024 * 20, ".", true),
						MemClustering.PerformanceLevel.SLOW), };
		MemClustering mclst = new MemClustering(ncs);
		MemBufferHolder<?> mbh;
		for (int idx = 1; idx <= 200; ++idx) {
			int size = randomGenerator.nextInt(1024 * 1024) + 1024 * 1024;
			mbh = mclst.createBuffer(size);
			for (int i = 0; i < size; i++) {
				mbh.get().put((byte) randomGenerator.nextInt(255));
			}
			assertEquals(size, mbh.get().capacity());
			// System.out.println("testMemByteBufferManualRelease");
			mbh.destroy();
		}
	}

	public boolean actriggered = false, pctriggered = false;

	/**
	 * test change events that will be triggered by memory pool downgrading or
	 * memory pool switching caused by fill up.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMemByteBufferWithChange() throws Exception {
		Random randomGenerator = new Random();
		MemClustering.NodeConfig<?> ncs[] = new MemClustering.NodeConfig<?>[] {
				new MemClustering.NodeConfig<SysMemAllocator>(new SysMemAllocator(
						1024 * 1024 * 20, true).disableActiveGC(),
						MemClustering.PerformanceLevel.FASTEST),
				new MemClustering.NodeConfig<BigDataMemAllocator>(new BigDataMemAllocator(
                                                Utils.getVolatileMemoryAllocatorService("vmem"),
						1024 * 1024 * 20, ".", true).disableActiveGC(),
						MemClustering.PerformanceLevel.FAST),
				// new MemClustering.NodeConfig(new
				// BigMemAllocator(1024*1024*20, ".", true).disableActiveGC(),
				// MemClustering.PerformanceLevel.NORMAL),
				new MemClustering.NodeConfig<BigDataMemAllocator>(new BigDataMemAllocator(
                                                Utils.getVolatileMemoryAllocatorService("vmem"),
						1024 * 1024 * 20, ".", true),
						MemClustering.PerformanceLevel.SLOW), };
		MemClustering mclst = new MemClustering(ncs){};

		actriggered = false;
		pctriggered = false;
		mclst.setAllocatorChange(new MemClustering.AllocatorChange() {
			@Override
			public void changed(MemClustering.PerformanceLevel lvl,
					CommonAllocator<?> prevallocator, CommonAllocator<?> tgtallocator) {
				System.out.println(String.format(
						"Allocator Changed: %s, %X -> %X", lvl.name(),
						System.identityHashCode(prevallocator),
						System.identityHashCode(tgtallocator)));
				actriggered = true;
			}
		});
		mclst.setPerformanceLevelChange(new MemClustering.PerformanceLevelChange() {
			@Override
			public void changed(MemClustering.PerformanceLevel prevlvl,
					MemClustering.PerformanceLevel lvl) {
				System.out.println(String.format(
						"Perf.Level Changed: %s -> %s",
						null == prevlvl ? "NULL" : prevlvl.name(), lvl.name()));
				pctriggered = true;
			}
		});

		MemBufferHolder<?> mbh;
		for (int idx = 1; idx <= 100; ++idx) {
			int size = randomGenerator.nextInt(1024 * 1024) + 1024 * 1024;
			mbh = mclst.createBuffer(size);
			for (int i = 0; i < size; i++) {
				mbh.get().put((byte) randomGenerator.nextInt(255));
			}
			//mbh.destroy();
		}
		assertTrue(actriggered && pctriggered);

	}

	/*
	 * @Test public void testMemChunk() { Random randomGenerator = new Random();
	 * Allocator act = new BigMemAllocator(1024*1024*1024, "/home/wg/bm", true);
	 * MemChunkHolder mch; for (int idx = 1; idx <= 50000; ++idx){ int size =
	 * randomGenerator.nextInt(1024*1024) + 1024*1024; mch =
	 * act.createChunk(size);
	 * System.out.println(String.format("[Seq.%d] addr : %X", idx, size,
	 * mch.get())); mch.destroy(); } }
	 */
}
