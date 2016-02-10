package com.intel.mnemonic.service.allocatorservice;

public interface NonVolatileMemoryAllocatorServiceFactory {

    public String getServiceId();
    
    public NonVolatileMemoryAllocatorService createServiceInstance();

}
