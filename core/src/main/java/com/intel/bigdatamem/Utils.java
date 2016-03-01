package com.intel.bigdatamem;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import sun.misc.Unsafe;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import com.intel.mnemonic.service.allocatorservice.VolatileMemoryAllocatorService;
import com.intel.mnemonic.service.allocatorservice.NonVolatileMemoryAllocatorService;

/**
 * <p>
 * Utilities for project.
 * </p>
 * 
 */
@SuppressWarnings("restriction")
public class Utils {
    private static long fSLEEP_INTERVAL = 100;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
	
    private static Unsafe m_unsafe = null;

    private static ServiceLoader<VolatileMemoryAllocatorService> m_vmasvcloader = null;
    private static ServiceLoader<NonVolatileMemoryAllocatorService> m_nvmasvcloader = null;

    /**
     * retrieve a volatile memory allocator service
     * 
     * @param id
     *         specify a name of allocator to retrieve
     *
     * @return the volatile memory allocator service instance
     */
    public static VolatileMemoryAllocatorService getVolatileMemoryAllocatorService(String id) {
	return getVolatileMemoryAllocatorService(id, true);
    }
    
    /**
     * retrieve a volatile memory allocator service
     * 
     * @param id
     *         specify a name of allocator to retrieve
     *
     * @param allownvmsvc
     *         specify whether allow to treat non-volatile memory allocator as volatile one during searching
     *
     * @return the volatile memory allocator service instance
     */
    public static VolatileMemoryAllocatorService getVolatileMemoryAllocatorService(String id, boolean allownvmsvc) {
	VolatileMemoryAllocatorService ret = null;
	if (null == m_vmasvcloader) {
	    m_vmasvcloader = ServiceLoader.load(VolatileMemoryAllocatorService.class);
	}
	Iterator<VolatileMemoryAllocatorService> svcit = m_vmasvcloader.iterator();
	VolatileMemoryAllocatorService svc = null;
	while (null == ret && svcit.hasNext()) {
	    svc = svcit.next();
	    if (svc.getServiceId().equals(id)) {
		ret = svc;
	    }
	}
	if (null == ret && allownvmsvc) {
	    ret = getNonVolatileMemoryAllocatorService(id);
	}
	assert null != ret : "VolatileMemoryAllocatorService \'" + id + "\' not found!";
	return ret;
    }

    /**
     * retrieve a non-volatile memory allocator service
     * 
     * @param id
     *         specify a name of allocator to retrieve
     *
     * @return the non-volatile memory allocator service instance
     */
    public static NonVolatileMemoryAllocatorService getNonVolatileMemoryAllocatorService(String id) {
	NonVolatileMemoryAllocatorService ret = null;
	if (null == m_nvmasvcloader) {
	    m_nvmasvcloader = ServiceLoader.load(NonVolatileMemoryAllocatorService.class);
	}
	Iterator<NonVolatileMemoryAllocatorService> svcit = m_nvmasvcloader.iterator();
	NonVolatileMemoryAllocatorService svc = null;
	while (null == ret && svcit.hasNext()) {
	    svc = svcit.next();
	    if (svc.getServiceId().equals(id)) {
		ret = svc;
	    }
	}
	assert null != ret : "NonVolatileMemoryAllocatorService \'" + id + "\' not found!";
	return ret;
    }

    /**
     * Generates a unique name that contains current timestamp.
     * 
     * @param format
     *            the template that is used to generate unique name.
     *
     * @return unique path name.
     */
    public static String genUniquePathname(String format) {
	String ret = null;
	if (null != format && !format.isEmpty()) {
	    ret = String.format(format, (new SimpleDateFormat(
							      "ddMMyy-hhmmss.SSS").format(new Date())));
	}
	return ret;
    }

    /**
     * retrieve the usage of memory.
     * 
     * @return the size of memory has been occupied
     */
    public static long getMemoryUse() {
	putOutTheGarbage();
	long totalMemory = Runtime.getRuntime().totalMemory();
	putOutTheGarbage();
	long freeMemory = Runtime.getRuntime().freeMemory();
	return (totalMemory - freeMemory);
    }

    /**
     * run garbage collections.
     */
    private static void putOutTheGarbage() {
	collectGarbage();
	collectGarbage();
    }

    /**
     * run a garbage collection.
     */
    public static void collectGarbage() {
	try {
	    System.gc();
	    Thread.sleep(fSLEEP_INTERVAL);
	    System.runFinalization();
	    Thread.sleep(fSLEEP_INTERVAL);
	} catch (InterruptedException ex) {
	    ex.printStackTrace();
	}
    }

    /**
     * Retrieve an Unsafe object.
     *
     * @throws Exception
     *        Error when get Unsafe object from runtime
     *
     * @return an unsafe object
     */
    public static Unsafe getUnsafe() throws Exception {
	if (null == m_unsafe) {
	    Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
	    field.setAccessible(true);
	    m_unsafe = (sun.misc.Unsafe) field.get(null);
	}
	return m_unsafe;
    }

    /**
     * resize a bytebuffer with a new instance
     *
     * @param buf
     *          specify a buf to resize
     *
     * @param size
     *          specify the size for resizing
     *
     * @return the resized bytebuffer instance
     */
    public static ByteBuffer resizeByteBuffer(ByteBuffer buf, long size) {
	ByteBuffer ret  = ByteBuffer.allocateDirect((int) size);
	if (ret != null) {
	    if (null != buf) {
		ret.put(buf);
		ret.flip();
	    }
	}
	return ret;
    }

    /**
     * create a new instance of Random using default fixed seed
     *
     * @return the instance of Random
     */
    public static Random createRandom() {
	return createRandom(0L);
    }

    /**
     * create a new instance of Random
     *
     * @param rgenseed
     *          specify a random seed
     *
     * @return the instance of Random
     */
    public static Random createRandom(long rgenseed) {
	Random ret = new Random();
	if (0L == rgenseed) {
	    rgenseed = System.currentTimeMillis();
	    System.out.println("Random number generator seed is " + rgenseed);
	} else {
	    System.out.println("Fixed Random number generator seed is " + rgenseed);
	}
	ret.setSeed(rgenseed);
	return ret;
    }

    /**
     * generate a random string with fixed length
     *
     * @return the random string
     */
    public static String genRandomString() {
	return genRandomString(6);
    }

    /**
     * generate a random string
     *
     * @param len
     *         specify the length of this random string
     *
     * @return the random string
     */
    public static String genRandomString(int len) {
	return UUID.randomUUID().toString().replaceAll("-", "").toUpperCase().substring(0, len);
    }

    /**
     * assert the equality of two generic objects using compareTo() operator
     *
     * @param <T> the type of comparable objects
     *
     * @param actual
     *          specify a object to be compared
     *
     * @param expected
     *          specify a object to be expected
     *
     * @return true if equal according to compareTo()
     */
    public static <T extends Comparable<T>>boolean assertComparison(T actual, T expected) {
	boolean ret = false;
	if ((expected == null) && (actual == null)) {
	    ret = true;
	} else if (expected != null) {
	    ret = expected.compareTo(actual) == 0;
	}
	return ret;
    }


}
