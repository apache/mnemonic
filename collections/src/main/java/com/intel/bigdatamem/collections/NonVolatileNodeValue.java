package com.intel.bigdatamem.collections;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.intel.bigdatamem.*;

/**
 * this class defines a non-volatile node for a generic value to form a unidirectional link
 *
 */
@NonVolatileEntity
public abstract class NonVolatileNodeValue<E> 
    implements Durable, Iterable<E> {
    protected transient EntityFactoryProxy[] m_node_efproxies;
    protected transient GenericField.GType[] m_node_gftypes;

    /**
     * creation callback for initialization
     *
     */
    @Override
    public void initializeAfterCreate() {
	//		System.out.println("Initializing After Created");
    }

    /**
     * restore callback for initialization
     *
     */
    @Override
    public void initializeAfterRestore() {
	//		System.out.println("Initializing After Restored");
    }

    /**
     * this function will be invoked by its factory to setup generic related info to avoid expensive operations from reflection
     *
     * @param efproxies
     *           specify a array of factory to proxy the restoring of its generic field objects
     *
     * @param gftypes
     *           specify a array of types corresponding to efproxies
     */
    @Override
    public void setupGenericInfo(EntityFactoryProxy[] efproxies, GenericField.GType[] gftypes) {
	m_node_efproxies = efproxies;
	m_node_gftypes = gftypes;
    }

    /**
     * get the item value of this node
     *
     * @return the item value of this node
     */
    @NonVolatileGetter(EntityFactoryProxies = "m_node_efproxies", GenericFieldTypes = "m_node_gftypes")
    abstract public E getItem();

    /**
     * set a value to this node item
     * 
     * @param value
     *          the value to be set
     *
     * @param destroy
     *          true if want to destroy exist one
     *
     */
    @NonVolatileSetter
    abstract public void setItem(E value, boolean destroy);

    /**
     * get next node
     *
     * @return the next node
     *
     */
    @NonVolatileGetter(EntityFactoryProxies = "m_node_efproxies", GenericFieldTypes = "m_node_gftypes")
    abstract public NonVolatileNodeValue<E> getNext();

    /**
     * set next node
     *
     * @param next
     *          specify the next node
     *
     * @param destroy
     *          true if want to destroy the exist node
     */
    @NonVolatileSetter
    abstract public void setNext(NonVolatileNodeValue<E> next, boolean destroy);
	
	
    /**
     * get an iterator instance of this list
     *
     * @return an iterator of this list
     */
    @Override
    public Iterator<E> 	iterator() {
	return new Intr(this);
    }
	
    /**
     * this class defines a iterator for this non-volatile list
     *
     */
    private class Intr implements Iterator<E> {
		
	protected NonVolatileNodeValue<E> next = null;

	/**
	 * Constructor
	 *
	 * @param head
	 *          the start point for this iterator
	 *
	 */
	Intr(NonVolatileNodeValue<E> head) {
	    next = head;
	}

	/**
	 * determine the existing of next
	 *
	 * @return true if there is a next node
	 *
	 */
	@Override
	public boolean hasNext() {
	    return null != next;
	}

	/**
	 * get next node
	 *
	 * @return the next node
	 */
	@Override
	public E next() {
	    if (null == next) {
		new NoSuchElementException();
	    }
	    E ret = next.getItem();
	    next = next.getNext();
	    return ret;
	}
    }
}
