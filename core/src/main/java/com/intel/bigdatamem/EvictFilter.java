package com.intel.bigdatamem;

/**
 * A listener callback to validate its accepted key-value pair for evacuation.
 * 
 */
public interface EvictFilter<KeyT, ValueT> {
    /**
     * A call-back validator when an entry will be evicted.
     *  
     * @param pool
     *            the pool which an entry has been evicted from
     *            
     * @param k
     *            the entry's key
     *            
     * @param v
     *            the entry's value
     *            
     * @return <tt>true</tt> if the entry is allowed to be dropped from its
     *         cache pool.
     */
    public boolean validate(CachePool<KeyT, ValueT> pool, KeyT k, ValueT v);
}
