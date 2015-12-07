package com.intel.bigdatamem;

/**
 *
 * @author Wang, Gang {@literal <gang1.wang@intel.com>}
 *
 */


public interface Durable {

	public void initializeAfterCreate();
	
	public void initializeAfterRestore();
	
	public void setupGenericInfo(EntityFactoryProxy[] efproxies, GenericField.GType[] gftypes);
	
	public void cancelAutoReclaim();

	public void registerAutoReclaim();
	
	public long getPersistentHandler();
	
	public boolean autoReclaim();

	public void destroy() throws RetrievePersistentEntityError;
}
