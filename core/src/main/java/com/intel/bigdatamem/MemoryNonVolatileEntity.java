package com.intel.bigdatamem;

/**
 *
 *
 */


public interface MemoryNonVolatileEntity<ALLOC_PMem3C93D24F59 extends CommonPersistAllocator<ALLOC_PMem3C93D24F59>> {

    public void initializeNonVolatileEntity(ALLOC_PMem3C93D24F59 allocator, EntityFactoryProxy[] efproxys, GenericField.GType[] gfields, boolean autoreclaim);
	
    public void createNonVolatileEntity(ALLOC_PMem3C93D24F59 allocator, EntityFactoryProxy[] efproxys, GenericField.GType[] gfields, boolean autoreclaim) 
	throws OutOfPersistentMemory;
	
    public void restoreNonVolatileEntity(ALLOC_PMem3C93D24F59 allocator, EntityFactoryProxy[] efproxys, GenericField.GType[] gfields, long phandler, boolean autoreclaim)
	throws RetrieveNonVolatileEntityError;
	
}
