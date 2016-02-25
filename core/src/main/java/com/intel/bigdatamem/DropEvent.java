package com.intel.bigdatamem;

/**
 * A event listener to monitor and post-process an entry's evacuation.
 * 
 * 
 */
public interface DropEvent<KeyT, ValueT> {
    /**
     * A call-back actor when an entry has been evicted. a customized drop
     * action can be implemented on this interface's method e.g. spill this
     * entry to disk or release associated resources etc.
     * 
     * @param pool
     *            the pool which an entry has been evicted from
     *            
     * @param k
     *            the key of an entry that has been evicted
     *            
     * @param v
     *            the value of an entry that has been evicted
     */
    public void drop(CachePool<KeyT, ValueT> pool, KeyT k, ValueT v);
}
