package com.intel.mnemonic.service.allocatorservice.internal;

import com.intel.mnemonic.service.allocatorservice.VolatileMemoryAllocatorServiceFactory;
import com.intel.mnemonic.service.allocatorservice.VolatileMemoryAllocatorService;

public class VMemServiceFactoryImpl implements VolatileMemoryAllocatorServiceFactory {

    public String getServiceId() {
        return "vmem";
    }

    public VolatileMemoryAllocatorService createServiceInstance() {
        return new VMemServiceImpl();
    }
    
}
