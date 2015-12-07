
package com.intel.bigdatamem;

/**
 * this Exception will be thrown once cache pool has been out of space.
 * 
 * @author Wang, Gang(Gary) {@literal <gang1.wang@intel.com>}
 */
public class ContainerOverflowException extends RuntimeException {

	private static final long serialVersionUID = -8515518456414990004L;

	/**
	 * accept a exception message to describe specific condition.
	 * 
	 * @param message
	 *            exception message
	 */
	public ContainerOverflowException(String message) {
		super(message);
	}
}
