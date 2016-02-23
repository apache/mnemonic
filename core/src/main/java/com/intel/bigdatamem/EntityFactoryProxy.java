package com.intel.bigdatamem;

/**
 * proxy the restoring of generic non-volatile object fields
 *
 */


public interface EntityFactoryProxy {

    /**
     * restore a durable object from persistent allocator using a handler of non-volatile object
     *
     * @param allocator
     *            specify a persistent allocator instance
     *
     * @param factoryproxys
     *            specify an array of factory proxies for its restored non-volatile object
     *
     * @param gfields
     *            specify an array of generic types of its generic fields corresponding to factoryproxys
     *
     * @param phandler
     *            specify a non-volatile handler to restore
     *
     * @param autoreclaim
     *            specify auto-reclaim for this restored non-volatile object
     *
     * @return the restored non-volatile object from this factory proxy
     *
     */
    public abstract <A extends CommonPersistAllocator<A>> Durable restore(A allocator, 
									  EntityFactoryProxy[] factoryproxys, GenericField.GType[] gfields, long phandler,
									  boolean autoreclaim);
}
