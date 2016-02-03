
package com.intel.bigdatamem;

/**
 * a interface to allocate big memory resource from any underlying memory kind
 * of storage and manage automatically reclaiming of memory resources.
 * 
 *
 */
public interface Allocator<A extends CommonAllocator<A>> extends Allocatable<A> {

	/**
	 * Release the memory pool and close it.
	 * 
	 * @see BigDataMemAllocator#close()
	 */
	public void close();

	/**
	 * @see BigDataMemAllocator#sync()
	 */
	public void sync();

	/**
	 * enable active garbage collection. the GC will be forced to perform when
	 * there is no more space to allocate.
	 * 
	 * @param timeout
	 *            the timeout is used to yield for GC performing
	 * 
	 * @return this object
	 * 
	 * @see BigDataMemAllocator#enableActiveGC(long)
	 */
	public A enableActiveGC(long timeout);

	/**
	 * disable active garbage collection.
	 * 
	 * @return this object
	 * 
	 * @see BigDataMemAllocator#disableActiveGC()
	 */
	public A disableActiveGC();

}
