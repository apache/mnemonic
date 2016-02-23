package com.intel.bigdatamem;

/**
 * an interface to manage the lifecycle of memory allocator
 *
 */
public interface Allocator<A extends CommonAllocator<A>> extends Allocatable<A> {

    /**
     * release the underlying memory pool and close it.
     * 
     */
    public void close();

    /**
     * sync. dirty data to underlying memory-like device
     *
     */
    public void sync();

    /**
     * enable active garbage collection. the GC will be forced to collect garbages when
     * there is no more space for current allocation request.
     *
     * @param timeout
     *            the timeout is used to yield for GC performing
     *
     * @return this allocator
     */
    public A enableActiveGC(long timeout);

    /**
     * disable active garbage collection.
     *
     * @return this allocator 
     */
    public A disableActiveGC();

}
