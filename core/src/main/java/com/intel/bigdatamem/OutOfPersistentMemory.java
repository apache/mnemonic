package com.intel.bigdatamem;

/**
 * this is an exception that should be throw once out of persistent memory
 *
 */

public class OutOfPersistentMemory extends RuntimeException {

    private static final long serialVersionUID = -6315943783592441148L;

    public OutOfPersistentMemory(String s) {
	super(s);
    }
}
