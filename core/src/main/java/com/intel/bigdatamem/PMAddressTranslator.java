package com.intel.bigdatamem;


/**
 * translate persistent memory address for allocator
 *
 */
public interface PMAddressTranslator {

    /**
     * calculate the portable address
     *
     * @param addr 
     *           the address to be calculated
     *
     * @return the portable address
     */
    public long getPortableAddress(long addr);

    /**
     * calculate the effective address
     *
     * @param addr 
     *           the address to be calculated
     *
     * @return the effective address
     */
    public long getEffectiveAddress(long addr);
	
    /**
     * get the base address
     *
     * @return the base address
     */
    public long getBaseAddress();
	
    /**
     * set the base address for calculation
     *
     * @param addr 
     *           the base address
     *
     */
    public long setBaseAddress(long addr);
}
