package com.intel.bigdatamem.collections;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.intel.bigdatamem.BigDataPMemAllocator;
import com.intel.bigdatamem.CommonPersistAllocator;
import com.intel.bigdatamem.Durable;
import com.intel.bigdatamem.EntityFactoryProxy;
import com.intel.bigdatamem.GenericField;
import com.intel.bigdatamem.Reclaim;
import com.intel.bigdatamem.Utils;

/**
 *
 *
 */


public class NonVolatileNodeValueNGTest {
	private long KEYCAPACITY;
	private Random m_rand;
	private BigDataPMemAllocator m_act;

	@BeforeClass
	public void setUp() {
		m_rand = Utils.createRandom();
		m_act = new BigDataPMemAllocator(Utils.getNonVolatileMemoryAllocatorService("pmalloc"), 1024 * 1024 * 1024, "./pobj_NodeValue.dat", true);
		KEYCAPACITY = m_act.handlerCapacity();
		m_act.setBufferReclaimer(new Reclaim<ByteBuffer>() {
			@Override
			public boolean reclaim(ByteBuffer mres, Long sz) {
				System.out.println(String.format("Reclaim Memory Buffer: %X  Size: %s", System.identityHashCode(mres),
						null == sz ? "NULL" : sz.toString()));
				return false;
			}
		});
		m_act.setChunkReclaimer(new Reclaim<Long>() {
			@Override
			public boolean reclaim(Long mres, Long sz) {
				System.out.println(String.format("Reclaim Memory Chunk: %X  Size: %s", System.identityHashCode(mres),
						null == sz ? "NULL" : sz.toString()));
				return false;
			}
		});
		
		for (long i = 0; i < KEYCAPACITY; ++i) {
			m_act.setHandler(i, 0L);
		}
	}
	
	@AfterClass
	public void tearDown() {
		m_act.close();
	}

	@Test(enabled = false)
	public void testSingleNodeValueWithInteger() {
		int val = m_rand.nextInt();
		GenericField.GType gtypes[] = {GenericField.GType.INTEGER}; 
		NonVolatileNodeValue<Integer> plln = NonVolatileNodeValueFactory.create(m_act, null, gtypes, false);
		plln.setItem(val, false);
		Long handler = plln.getNonVolatileHandler();
		System.err.println("-------------Start to Restore Integer -----------");
		NonVolatileNodeValue<Integer> plln2 = NonVolatileNodeValueFactory.restore(m_act, null, gtypes, handler, false);
		AssertJUnit.assertEquals(val, (int)plln2.getItem());
	}
	
	@Test(enabled = false)
	public void testNodeValueWithString() {
		String val = Utils.genRandomString();
		GenericField.GType gtypes[] = {GenericField.GType.STRING}; 
		NonVolatileNodeValue<String> plln = NonVolatileNodeValueFactory.create(m_act, null, gtypes, false);
		plln.setItem(val, false);
		Long handler = plln.getNonVolatileHandler();
		System.err.println("-------------Start to Restore String-----------");
		NonVolatileNodeValue<String> plln2 = NonVolatileNodeValueFactory.restore(m_act, null, gtypes, handler, false);
		AssertJUnit.assertEquals(val, plln2.getItem());
	}
	
	@Test(enabled = false)
	public void testNodeValueWithPerson() {

		Person<Long> person = PersonFactory.create(m_act);
		person.setAge((short)31);
		
		GenericField.GType gtypes[] = {GenericField.GType.DURABLE}; 
		EntityFactoryProxy efproxies[] = {new EntityFactoryProxy(){
			@Override
			public <A extends CommonPersistAllocator<A>> Durable restore(A allocator,
					EntityFactoryProxy[] factoryproxys, GenericField.GType[] gfields, long phandler, boolean autoreclaim) {
				return PersonFactory.restore(allocator, factoryproxys, gfields, phandler, autoreclaim);
			    }
			}
		};
		
		NonVolatileNodeValue<Person<Long>> plln = NonVolatileNodeValueFactory.create(m_act, efproxies, gtypes, false);
		plln.setItem(person, false);
		Long handler = plln.getNonVolatileHandler();
		
		NonVolatileNodeValue<Person<Long>> plln2 = NonVolatileNodeValueFactory.restore(m_act, efproxies, gtypes, handler, false);
		AssertJUnit.assertEquals(31, (int)plln2.getItem().getAge());

	}
	@Test(enabled = false)
	public void testLinkedNodeValueWithPerson() {

		int elem_count = 10;
		List<Long> referlist = new ArrayList();

		GenericField.GType listgftypes[] = {GenericField.GType.DURABLE}; 
		EntityFactoryProxy listefproxies[] = { 
				new EntityFactoryProxy(){
						@Override
						public <A extends CommonPersistAllocator<A>> Durable restore(A allocator,
								EntityFactoryProxy[] factoryproxys, GenericField.GType[] gfields, long phandler, boolean autoreclaim) {
							return PersonFactory.restore(allocator, factoryproxys, gfields, phandler, autoreclaim);
						    }
						}
				};
		
		NonVolatileNodeValue<Person<Long>> firstnv = NonVolatileNodeValueFactory.create(m_act, listefproxies, listgftypes, false);
		
		NonVolatileNodeValue<Person<Long>> nextnv = firstnv;
		
		Person<Long> person;
		long val;
		NonVolatileNodeValue<Person<Long>> newnv;
		for (int i = 0; i < elem_count; ++i) {
			person = PersonFactory.create(m_act);
			person.setAge((short)m_rand.nextInt(50));
			person.setName(String.format("Name: [%s]", Utils.genRandomString()), true);
			nextnv.setItem(person, false);
			newnv = NonVolatileNodeValueFactory.create(m_act, listefproxies, listgftypes, false);
			nextnv.setNext(newnv, false);
			nextnv = newnv;
		}
		
		Person<Long> eval;
		NonVolatileNodeValue<Person<Long>> iternv = firstnv;
		while(null != iternv) {
			System.out.printf(" Stage 1 --->\n");
		    eval = iternv.getItem();
			if (null != eval)
				eval.testOutput();
			iternv = iternv.getNext();
		}
		
		long handler = firstnv.getNonVolatileHandler();
		
		NonVolatileNodeValue<Person<Long>> firstnv2 = NonVolatileNodeValueFactory.restore(m_act, listefproxies, listgftypes, handler, false);
		
		for (Person<Long> eval2 : firstnv2) {
			System.out.printf(" Stage 2 ---> \n");
			if (null != eval2)
				eval2.testOutput();
		}
		
		//Assert.assert, expected);(plist, plist2);
		
	}
	
	@Test(enabled = true)
	public void testLinkedNodeValueWithLinkedNodeValue() {

		int elem_count = 10;
		long slotKeyId = 10;

		GenericField.GType[] elem_gftypes = {GenericField.GType.DOUBLE};
        EntityFactoryProxy[] elem_efproxies = null;

		GenericField.GType linkedgftypes[] = {GenericField.GType.DURABLE, GenericField.GType.DOUBLE};
		EntityFactoryProxy linkedefproxies[] = { 
				new EntityFactoryProxy(){
						@Override
						public <A extends CommonPersistAllocator<A>> Durable restore(A allocator,
								EntityFactoryProxy[] factoryproxys, GenericField.GType[] gfields, long phandler, boolean autoreclaim) {
								EntityFactoryProxy[] val_efproxies = null;
								GenericField.GType[] val_gftypes = null;
								if ( null != factoryproxys && factoryproxys.length >= 2 ) {
									val_efproxies = Arrays.copyOfRange(factoryproxys, 1, factoryproxys.length);
								}
								if ( null != gfields && gfields.length >= 2 ) {
									val_gftypes = Arrays.copyOfRange(gfields, 1, gfields.length);
								}
								return NonVolatileNodeValueFactory.restore(allocator, val_efproxies, val_gftypes, phandler, autoreclaim);
						    }
						}
				};
		
		NonVolatileNodeValue<NonVolatileNodeValue<Double>> nextnv = null, pre_nextnv = null;
		NonVolatileNodeValue<Double> elem = null, pre_elem = null, first_elem = null;
		
		Long linkhandler = 0L;
		
		System.out.printf(" Stage 1 -testLinkedNodeValueWithLinkedNodeValue--> \n");

		pre_nextnv = null;
		Double val;
		for (int i=0; i< elem_count; ++i) {
			first_elem = null;
			pre_elem = null;
			for (int v=0; v<3 ; ++v) {
				elem = NonVolatileNodeValueFactory.create(m_act, elem_efproxies, elem_gftypes, false);
				val = m_rand.nextDouble();
				elem.setItem(val, false);
				if (null == pre_elem) {
					first_elem = elem;
				} else {
					pre_elem.setNext(elem, false);
				}
				pre_elem = elem;
				System.out.printf("%f ", val);
			}
			
			nextnv = NonVolatileNodeValueFactory.create(m_act, linkedefproxies, linkedgftypes, false);
			nextnv.setItem(first_elem, false);
			if (null == pre_nextnv) {
				linkhandler = nextnv.getNonVolatileHandler();
			} else {
				pre_nextnv.setNext(nextnv, false);
			}
			pre_nextnv = nextnv;
			System.out.printf(" generated an item... \n");
		}
		m_act.setHandler(slotKeyId, linkhandler);
		
		long handler = m_act.getHandler(slotKeyId);
		
		NonVolatileNodeValue<NonVolatileNodeValue<Double>> linkedvals = NonVolatileNodeValueFactory.restore(m_act, linkedefproxies, linkedgftypes, handler, false);
		Iterator<NonVolatileNodeValue<Double>> iter = linkedvals.iterator();
		Iterator<Double> elemiter = null;
		
		System.out.printf(" Stage 2 -testLinkedNodeValueWithLinkedNodeValue--> \n");
		while(iter.hasNext()) {
			elemiter = iter.next().iterator();
			while(elemiter.hasNext()) {
				System.out.printf("%f ", elemiter.next());
			}
			System.out.printf(" Fetched an item... \n");
		}
		
		//Assert.assert, expected);(plist, plist2);
		
	}

}
