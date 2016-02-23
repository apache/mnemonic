package com.intel.bigdatamem;

/**
 * this interface defines the interactive functionalities with Mnenomic core part.
 *
 */


public interface Durable {

    /**
     * this function will be invoked after this non-volatile object is created brandly new 
     *
     */
    public void initializeAfterCreate();

    /**
     * this function will be invoked after this non-volatile object is restored from its allocator 
     *
     */
    public void initializeAfterRestore();
	

    /**
     * this function will be invoked by its factory to setup generic related info to avoid expensive operations from reflection
     *
     * @param efproxies
     *           specify a array of factory to proxy the restoring of its generic field objects
     *
     * @param gftypes
     *           specify a array of types corresponding to efproxies
     */
    public void setupGenericInfo(EntityFactoryProxy[] efproxies, GenericField.GType[] gftypes);

    /**
     * this function could be called by user code to disable auto-reclaim for this non-volatile object
     *
     */
    public void cancelAutoReclaim();

    /**
     * this function could be called by user code to register this object for auto-reclaim 
     *
     */
    public void registerAutoReclaim();

    /**
     * this function returns its bound handler for this object
     *
     * @return the handler of this object
     */
    public long getNonVolatileHandler();

    /**
     * return the setting for auto-reclaim
     *
     * @return the status of the auto-reclaim setting
     */
    public boolean autoReclaim();

    /**
     * manually destroy this object and release its memory resource
     *
     */
    public void destroy() throws RetrieveNonVolatileEntityError;
}
