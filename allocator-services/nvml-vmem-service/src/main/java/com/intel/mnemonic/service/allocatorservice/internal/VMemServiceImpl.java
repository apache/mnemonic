package com.intel.mnemonic.service.allocatorservice.internal;

import com.intel.mnemonic.service.allocatorservice.VolatileMemoryAllocatorService;
import org.flowcomputing.commons.primitives.*;
import java.nio.ByteBuffer;


public class VMemServiceImpl implements VolatileMemoryAllocatorService {
    static {
        try {
            NativeLibraryLoader.loadFromJar("vmemallocator");
        } catch (Exception e) {
            System.exit(-1);
        }
    }

    public String getServiceId() {
        return "vmem";
    }
    
    public long init(long capacity, String uri, boolean isnew) {
        return ninit(capacity, uri, isnew);
    }


    public void close(long id) {
        nclose(id);
    }


    public void sync(long id) {
        nsync(id);
    }


    public long allocate(long id, long size, boolean initzero) {
        return nallocate(id, size, initzero);
    }


    public long reallocate(long id, long address, long size, boolean initzero) {
        return nreallocate(id, address, size, initzero);
    }


    public void free(long id, long address) {
        nfree(id, address);
    }


    public ByteBuffer createByteBuffer(long id, long size) {
        return ncreateByteBuffer(id, size);
    }


    public  ByteBuffer resizeByteBuffer(long id, ByteBuffer bytebuf, long size) {
        return nresizeByteBuffer(id, bytebuf, size);
    }

    public void destroyByteBuffer(long id, ByteBuffer bytebuf) {
        ndestroyByteBuffer(id, bytebuf);
    }


    protected native long ninit(long capacity, String uri, boolean isnew);


    protected native void nclose(long id);


    protected native void nsync(long id);


    protected native long nallocate(long id, long size, boolean initzero);


    protected native long nreallocate(long id, long address, long size, boolean initzero);


    protected native void nfree(long id, long address);


    protected native ByteBuffer ncreateByteBuffer(long id, long size);


    protected native ByteBuffer nresizeByteBuffer(long id, ByteBuffer bytebuf, long size);
	

    protected native void ndestroyByteBuffer(long id, ByteBuffer bytebuf);

}
