
package com.intel.bigdatamem.collections;

/**
 * a dummy object that is used for other test cases.
 * 
 * 
 *
 */
public class Payload implements java.io.Serializable, Comparable<Payload> {
	
	private static final long serialVersionUID = 187397440699436500L;

	public Payload(int iv, String strv, double dv) {
		ival = iv;
		strval = strv;
		dval = dv;
	}

	public int ival;
	public String strval;
	public double dval;

	@Override
	public int compareTo(Payload pl) {
		return ival == pl.ival && strval.equals(pl.strval) && dval == pl.dval ? 0
				: 1;
	}
}
