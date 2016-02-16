package com.intel.bigdatamem;

/**
 *
 *
 */


import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import com.intel.bigdatamem.Utils;

import org.testng.annotations.Test;

public class NonVolatilePersonNGTest {
  private long KEYCAPACITY;

  @Test(expectedExceptions = { OutOfPersistentMemory.class })
  public void testGenPeople() throws OutOfPersistentMemory, RetrieveNonVolatileEntityError {
	Random rand = Utils.createRandom();
	BigDataPMemAllocator act = new BigDataPMemAllocator(Utils.getNonVolatileMemoryAllocatorService("pmalloc"), 1024 * 1024 * 8, "./pobj_person.dat", true);
	KEYCAPACITY = act.handlerCapacity();
	act.setBufferReclaimer(new Reclaim<ByteBuffer>() {
		@Override
		public boolean reclaim(ByteBuffer mres, Long sz) {
			System.out.println(String.format(
					"Reclaim Memory Buffer: %X  Size: %s", System
							.identityHashCode(mres),
					null == sz ? "NULL" : sz.toString()));
			return false;
		}
	});
	act.setChunkReclaimer(new Reclaim<Long>() {
		@Override
		public boolean reclaim(Long mres, Long sz) {
			System.out.println(String.format(
					"Reclaim Memory Chunk: %X  Size: %s", System
							.identityHashCode(mres),
					null == sz ? "NULL" : sz.toString()));
			return false;
		}
	});
	
	for (long i = 0; i < KEYCAPACITY; ++i) {
		act.setHandler(i, 0L);
	}
	
	Person<Integer> mother;
	Person<Integer> person;
	
	long keyidx = 0;
	long val;
	
	try {
		while(true) {
			//if (keyidx >= KEYCAPACITY) break;
			
			keyidx %= KEYCAPACITY;
			
			System.out.printf("************ Generating People on Key %d ***********\n", keyidx);
			
			val = act.getHandler(keyidx);
			if (0L != val) {
				PersonFactory.restore(act, val, true);
			}
			
			person = PersonFactory.create(act);
			person.setAge((short)rand.nextInt(50));
			person.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);
			person.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);
			person.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);
			person.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);
			
			act.setHandler(keyidx, person.getNonVolatileHandler());
			
			for (int deep = 0; deep < rand.nextInt(100); ++deep) {
						
				mother = PersonFactory.create(act);
				mother.setAge((short)(50 + rand.nextInt(50)));
				mother.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);
				
				person.setMother(mother, true);
				
				person = mother;
				
			}
			++keyidx;
		}
	}finally {
		act.close();
	}
  }

  @Test(dependsOnMethods = {"testGenPeople"})
  public void testCheckPeople() throws RetrieveNonVolatileEntityError {
	BigDataPMemAllocator act = new BigDataPMemAllocator(Utils.getNonVolatileMemoryAllocatorService("pmalloc"), 1024 * 1024 * 8, "./pobj_person.dat", true);
	act.setBufferReclaimer(new Reclaim<ByteBuffer>() {
		@Override
		public boolean reclaim(ByteBuffer mres, Long sz) {
			System.out.println(String.format(
					"Reclaim Memory Buffer: %X  Size: %s", System
							.identityHashCode(mres),
					null == sz ? "NULL" : sz.toString()));
			return false;
		}
	});
	act.setChunkReclaimer(new Reclaim<Long>() {
		@Override
		public boolean reclaim(Long mres, Long sz) {
			System.out.println(String.format(
					"Reclaim Memory Chunk: %X  Size: %s", System
							.identityHashCode(mres),
					null == sz ? "NULL" : sz.toString()));
			return false;
		}
	});

	long val;
	for (long i = 0; i < KEYCAPACITY; ++i) {
		System.out.printf("----------Key %d--------------\n", i);
		val = act.getHandler(i);
		if (0L == val) {
			break;
		}
		Person<Integer> person = PersonFactory.restore(act, val, true);
		while (null != person) {
			person.testOutput();
			person = person.getMother();
		}
	}
	
	act.close();
  }  
}
