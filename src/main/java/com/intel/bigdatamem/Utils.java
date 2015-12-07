package com.intel.bigdatamem;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import sun.misc.Unsafe;

/**
 * <p>
 * A utilities library.
 * </p>
 * 
 * @author Wang, Gang(Gary) {@literal <gang1.wang@intel.com>}
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
	
	private static Unsafe m_unsafe;
	
	/**
	 * Generates a unique name that contains current timestamp.
	 * 
	 * @param format
	 *            the template that is used to generate unique name.
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
	 * @return the size of memory has been used
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

	public static Random createRandom() {
		return createRandom(0L);
	}

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
	
	public static String genRandomString() {
		return genRandomString(6);
	}
	
	public static String genRandomString(int len) {
		return UUID.randomUUID().toString().replaceAll("-", "").toUpperCase().substring(0, len);
	}
	
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
