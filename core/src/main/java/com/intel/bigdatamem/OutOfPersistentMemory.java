package com.intel.bigdatamem;

/**
 *
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
