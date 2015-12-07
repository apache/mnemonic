package com.intel.bigdatamem;

/**
 *
 * @author Wang, Gang {@literal <gang1.wang@intel.com>}
 *
 */


public interface MemoryPersistentEntity<ALLOC_PMem3C93D24F59 extends CommonPersistAllocator<ALLOC_PMem3C93D24F59>> {

	public void initializePersistentEntity(ALLOC_PMem3C93D24F59 allocator, EntityFactoryProxy[] efproxys, GenericField.GType[] gfields, boolean autoreclaim);
	
	public void createPersistentEntity(ALLOC_PMem3C93D24F59 allocator, EntityFactoryProxy[] efproxys, GenericField.GType[] gfields, boolean autoreclaim) 
			throws OutOfPersistentMemory;
	
	public void restorePersistentEntity(ALLOC_PMem3C93D24F59 allocator, EntityFactoryProxy[] efproxys, GenericField.GType[] gfields, long phandler, boolean autoreclaim)
			throws RetrievePersistentEntityError;
	
}
