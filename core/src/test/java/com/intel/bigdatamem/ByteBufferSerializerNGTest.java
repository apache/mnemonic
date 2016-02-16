
package com.intel.bigdatamem;

import static org.testng.Assert.*;
import org.testng.annotations.Test;

import com.intel.bigdatamem.BigDataMemAllocator;
import com.intel.bigdatamem.ByteBufferSerializer;
import com.intel.bigdatamem.MemBufferHolder;
import com.intel.bigdatamem.Utils;

import java.nio.ByteBuffer;
import java.util.Random;
import java.io.IOException;

/**
 * test the functionalities of ByteBufferSerializer class
 * 
 * @author Wang, Gang(Gary) {@literal <gang1.wang@intel.com>}
 */
public class ByteBufferSerializerNGTest {

	/**
	 * test to convert any serializable object from/to ByteBuffer object that is
	 * backed by Java heap.
	 */
	@Test
	public void testToFromByteBuffer() throws IOException,
			ClassNotFoundException {
		Random randomGenerator = new Random();
		for (int idx = 0; idx < 100; idx++) {
			Payload pl = new Payload(randomGenerator.nextInt(1024 * 1024),
					String.format("Str is %d",
							randomGenerator.nextInt(1024 * 1024)),
					randomGenerator.nextDouble());
			ByteBuffer bb = ByteBufferSerializer.toByteBuffer(pl);
			Payload rpl = ByteBufferSerializer.toObject(bb);
			assertTrue(pl.compareTo(rpl) == 0);
		}
	}

	/**
	 * test to convert any serializable object from/to MemBufferHolder object
	 * that is backed by native memory pool.
	 */
	@Test
	public void testToFromMemBufferHolder() throws IOException,
			ClassNotFoundException {
                BigDataMemAllocator act = new BigDataMemAllocator(Utils.getVolatileMemoryAllocatorService("vmem"), 1024 * 1024 * 1024, ".", true);

		Random randomGenerator = new Random();
		for (int idx = 0; idx < 100; idx++) {
			Payload pl = new Payload(randomGenerator.nextInt(1024 * 1024),
					String.format("Str is %d",
							randomGenerator.nextInt(1024 * 1024)),
					randomGenerator.nextDouble());
			MemBufferHolder<BigDataMemAllocator> mbh = 
					ByteBufferSerializer.toMemBufferHolder(act,	pl);
			Payload rpl = ByteBufferSerializer.fromMemBufferHolder(mbh);
			mbh.destroy();
			assertTrue(pl.compareTo(rpl) == 0);
		}

	}
}
