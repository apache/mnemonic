package com.intel.mnemonic.service.allocatorservice.internal;

import com.intel.mnemonic.service.allocatorservice.NonVolatileMemoryAllocatorServiceFactory;
import com.intel.mnemonic.service.allocatorservice.NonVolatileMemoryAllocatorService;

public class PMallocServiceFactoryImpl implements NonVolatileMemoryAllocatorServiceFactory {

    public String getServiceId() {
        return "pmalloc";
    }

    public NonVolatileMemoryAllocatorService createServiceInstance() {
        return new PMallocServiceImpl();
    }
    
}
