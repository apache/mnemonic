
package com.intel.bigdatamem;

import org.testng.annotations.Test;

import com.intel.bigdatamem.Allocator;
import com.intel.bigdatamem.BigDataMemAllocator;
import com.intel.bigdatamem.MemBufferHolder;
import com.intel.bigdatamem.MemChunkHolder;
import com.intel.bigdatamem.Utils;

import java.util.Random;

/**
 * test the functionality of BigMemAllocator class.
 * 
 * @author Wang, Gang(Gary) {@literal <gang1.wang@intel.com>}
 */
public class BigDataMemAllocatorNGTest {
	/**
	 * test to allocate MemBufferHolder objects and then verify them.
	 */
	@Test
	public void testMemByteBuffer() {
		Random randomGenerator = new Random();
		Allocator<BigDataMemAllocator> act = new BigDataMemAllocator(Utils.getVolatileMemoryAllocatorService("vmem"), 1024 * 1024 * 1024, ".", true);
		MemBufferHolder<?> mbh;
		for (int idx = 1; idx <= 5; ++idx) {
			int size = randomGenerator.nextInt(1024 * 1024) + 1024 * 1024;
			mbh = act.createBuffer(size);
			for (int i = 0; i < size; i++) {
				mbh.get().put((byte) randomGenerator.nextInt(255));
			}
			// if (bb.hasArray()) randomGenerator.nextBytes(bb.array());
			System.out.println(String.format("[Seq.%d] size %d - %d, (%s)",
					idx, size, mbh.get().capacity(), size == mbh.get()
							.capacity() ? "Correct" : "Failed!!!"));
			// mbh.destroy();
		}
	}

	/**
	 * test to allocate MemChunkHolder objects and then verify them.
	 */
	@Test
	public void testMemChunk() {
		Random randomGenerator = new Random();
		Allocator<BigDataMemAllocator> act = new BigDataMemAllocator(Utils.getVolatileMemoryAllocatorService("vmem"), 1024 * 1024 * 1024, ".", true);
		MemChunkHolder<?> mch;
		for (int idx = 1; idx <= 5; ++idx) {
			int size = randomGenerator.nextInt(1024 * 1024) + 1024 * 1024;
			mch = act.createChunk(size);
			System.out.println(String.format("[Seq.%d] addr : %X", idx, size,
					mch.get()));
			mch.destroy();
		}
	}

}
