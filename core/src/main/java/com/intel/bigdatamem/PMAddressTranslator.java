package com.intel.bigdatamem;


/**
 *
 *
 */


public interface PMAddressTranslator {
	
	public long getPortableAddress(long addr);
	
	public long getEffectiveAddress(long addr);
	
	public long getBaseAddress();
	
	public long setBaseAddress(long addr);
}
