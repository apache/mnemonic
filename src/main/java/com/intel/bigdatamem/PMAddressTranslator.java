package com.intel.bigdatamem;


/**
 *
 * @author Wang, Gang {@literal <gang1.wang@intel.com>}
 *
 */


public interface PMAddressTranslator {
	
	public long getPortableAddress(long addr);
	
	public long getEffectiveAddress(long addr);
	
	public long getBaseAddress();
	
	public long setBaseAddress(long addr);
}
