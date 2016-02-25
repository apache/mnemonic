package com.intel.bigdatamem;

import org.flowcomputing.commons.resgc.*;

/**
 * hold a memory kind of resource.
 * 
 */
public abstract class MemHolder<A extends CommonAllocator<A>, T, H extends MemHolder<A, T, H>> extends ResHolder<T, H> {

    protected A m_allocator;
	
    /**
     * Constructor: initialize with resource.
     * 
     * @param mres
     *            specify a resource to be holden
     *            
     * @param ar
     *            specify an Allocator for this holder
     *            
     */
    public MemHolder(T mres, A ar) {
	super(mres);
	m_allocator = ar;
    }

    /**
     * get its allocator
     *
     * @return the allocator
     */
    public A getAllocator() {
	return m_allocator;
    }

    /**
     * resize its held resource
     *
     * @param size
     *          specify the new size for its held resource
     */
    abstract public MemHolder<A, T, H> resize(long size);

    /**
     * get the size of its held memory resource.
     * 
     * @return the size
     */
    abstract public long getSize();

    /**
     * register its held resource for auto-reclaim
     *
     */
    abstract public void registerAutoReclaim();

}
