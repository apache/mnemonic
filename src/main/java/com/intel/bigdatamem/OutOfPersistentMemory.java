package com.intel.bigdatamem;

/**
 *
 * @author Wang, Gang {@literal <gang1.wang@intel.com>}
 *
 */

public class OutOfPersistentMemory extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6315943783592441148L;

	public OutOfPersistentMemory(String s) {
		super(s);
	}
}
