package com.intel.bigdatamem;

import org.flowcomputing.commons.resgc.*;

/**
 * hold a memory kind of resource.
 * 
 *
 */
public abstract class MemHolder<A extends CommonAllocator<A>, T, H extends MemHolder<A, T, H>> extends ResHolder<T, H> {

	protected A m_allocator;
	
	/**
	 * initialize the MemHolder object.
	 * 
	 * @param mres
	 *            specify the resource to be holden
	 *            
	 * @param ar
	 *            specify an Allocator for this holder
	 *            
	 */
	public MemHolder(T mres, A ar) {
		super(mres);
		m_allocator = ar;
	}
	
	public A getAllocator() {
		return m_allocator;
	}
	
	abstract public MemHolder<A, T, H> resize(long size);

	/**
	 * get the size of its holden memory resource.
	 * 
	 * @return the size of its holden memory resource
	 */
	abstract public long getSize();

	abstract public void registerAutoReclaim();

}
