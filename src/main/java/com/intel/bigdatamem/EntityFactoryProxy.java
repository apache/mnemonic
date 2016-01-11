package com.intel.bigdatamem;

/**
 *
 *
 */


public interface EntityFactoryProxy {
	public abstract <A extends CommonPersistAllocator<A>> Durable restore(A allocator, 
			EntityFactoryProxy[] factoryproxys, GenericField.GType[] gfields, long phandler,
			boolean autoreclaim);
}
