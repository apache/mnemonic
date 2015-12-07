package com.intel.bigdatamem;

/**
 *
 * @author Wang, Gang {@literal <gang1.wang@intel.com>}
 *
 */


public interface EntityFactoryProxy {
	public abstract <A extends CommonPersistAllocator<A>> Durable restore(A allocator, 
			EntityFactoryProxy[] factoryproxys, GenericField.GType[] gfields, long phandler,
			boolean autoreclaim);
}
