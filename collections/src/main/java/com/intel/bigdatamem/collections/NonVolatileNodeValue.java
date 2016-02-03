package com.intel.bigdatamem.collections;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.intel.bigdatamem.*;

/**
 *
 *
 */


@NonVolatileEntity
public abstract class NonVolatileNodeValue<E> 
    implements Durable, Iterable<E> {
	protected transient EntityFactoryProxy[] m_node_efproxies;
	protected transient GenericField.GType[] m_node_gftypes;
	
	@Override
	public void initializeAfterCreate() {
//		System.out.println("Initializing After Created");
	}

	@Override
	public void initializeAfterRestore() {
//		System.out.println("Initializing After Restored");
	}
	
	@Override
	public void setupGenericInfo(EntityFactoryProxy[] efproxies, GenericField.GType[] gftypes) {
		m_node_efproxies = efproxies;
		m_node_gftypes = gftypes;
	}
	
	@NonVolatileGetter(EntityFactoryProxies = "m_node_efproxies", GenericFieldTypes = "m_node_gftypes")
	abstract public E getItem();
	@NonVolatileSetter
	abstract public void setItem(E next, boolean destroy);
	
	@NonVolatileGetter(EntityFactoryProxies = "m_node_efproxies", GenericFieldTypes = "m_node_gftypes")
	abstract public NonVolatileNodeValue<E> getNext();
	@NonVolatileSetter
	abstract public void setNext(NonVolatileNodeValue<E> next, boolean destroy);
	
	
	
	@Override
	public Iterator<E> 	iterator() {
		return new Intr(this);
	}
	
	private class Intr implements Iterator<E> {
		
		protected NonVolatileNodeValue<E> next = null;
		
		Intr(NonVolatileNodeValue<E> head) {
			next = head;
		}
		
		@Override
		public boolean hasNext() {
			return null != next;
		}
		
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
