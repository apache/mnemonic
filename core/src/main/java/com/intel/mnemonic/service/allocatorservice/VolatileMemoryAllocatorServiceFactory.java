package com.intel.mnemonic.service.allocatorservice;

public interface VolatileMemoryAllocatorServiceFactory {

    public String getServiceId();
    
    public VolatileMemoryAllocatorService createServiceInstance();

}
