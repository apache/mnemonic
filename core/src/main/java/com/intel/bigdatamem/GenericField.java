package com.intel.bigdatamem;

/**
 * this class defines generic field for non-volatile entity
 *
 */

import sun.misc.Unsafe;

@SuppressWarnings("restriction")
public class GenericField<A extends CommonPersistAllocator<A>, E> implements Durable {

    /**
     * defines the types of generic field
     *
     */
    public enum GType {
	BOOLEAN, CHARACTER, BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, STRING, DURABLE
    };

    private Unsafe m_unsafe;
    private long m_fpos;
    private GType m_dgftype = null;
    private Durable m_field = null;
    private MemBufferHolder<A> m_strfield = null;
    private A m_allocator;
    private boolean m_autoreclaim;
    private EntityFactoryProxy m_defproxy = null;
    private EntityFactoryProxy[] m_efproxies;
    private GType[] m_gftypes;

    /**
     * Constructor: initialize this generic field
     *
     * @param defproxy
     *           specify its entity factory proxy
     *
     * @param dgftype
     *           specify its type
     *
     * @param efproxies
     *           specify an array of containing entity factory proxies
     *
     * @param gftypes
     *           specify an array of containing types corresponding to dfproxies
     *
     * @param allocator
     *           specify the allocator this field sit on
     *
     * @param unsafe
     *           specify the unsafe instance
     *
     * @param autoreclaim
     *           specify true if ask for auto-reclaim for this field
     *
     * @param fpos
     *           specify the field position
     *
     */
    public GenericField(EntityFactoryProxy defproxy, GType dgftype, EntityFactoryProxy[] efproxies, GType[] gftypes, 
			A allocator, Unsafe unsafe, boolean autoreclaim, Long fpos) {
	m_unsafe = unsafe;
	m_fpos = fpos;
	m_allocator = allocator;
	m_autoreclaim = autoreclaim;
	m_efproxies = efproxies;
	m_gftypes = gftypes;
	m_defproxy = defproxy;
	m_dgftype = dgftype;
    }

    /**
     * set a value to this field
     *
     * @param e
     *        specify a value to set
     *
     * @param destroy
     *        specify true if want to destroy the original value
     */
    public void set(E e, boolean destroy) {
	boolean isnull = null == e;
	switch (m_dgftype) {
	case BYTE:
	    m_unsafe.putByte(m_fpos, isnull ? (byte)0 : (Byte) e);
	    break;
	case BOOLEAN:
	    m_unsafe.putByte(m_fpos, isnull ? (byte)0 : ((Boolean) e ? (byte)1 : (byte)0));
	    break;
	case CHARACTER:
	    m_unsafe.putChar(m_fpos, isnull ? (char)0 : (Character) e);
	    break;
	case SHORT:
	    m_unsafe.putShort(m_fpos, isnull ? (short)0 : (Short) e);
	    break;
	case INTEGER:
	    m_unsafe.putInt(m_fpos, isnull ? (int)0 : (Integer) e);
	    break;
	case LONG:
	    m_unsafe.putLong(m_fpos, isnull ? (long)0 : (Long) e);
	    break;
	case FLOAT:
	    m_unsafe.putFloat(m_fpos, isnull ? (float)0 : (Float) e);
	    break;
	case DOUBLE:
	    m_unsafe.putDouble(m_fpos, isnull ? (double)0 : (Double) e);
	    break;
	case STRING:
	    String str = (String) e;
	    if (destroy && null != get()) {
		m_strfield.destroy();
		m_strfield = null;
		m_unsafe.putAddress(m_fpos, 0L);
	    }
	    if (null == str) {
		m_unsafe.putAddress(m_fpos, 0L);
	    } else {
		m_strfield = m_allocator.createBuffer(str.length() * 2, m_autoreclaim);
		if (null == m_strfield) {
		    throw new OutOfPersistentMemory("Create Persistent String Error!");
		}
		m_strfield.get().asCharBuffer().put(str);
		m_unsafe.putAddress(m_fpos, m_allocator.getBufferHandler(m_strfield));
	    }
	    break;
	case DURABLE:
	    if (destroy && null != get()) {
		m_field.destroy();
		m_field = null;
		m_unsafe.putAddress(m_fpos, 0L);
	    }
	    m_field = (Durable) e;
	    m_unsafe.putAddress(m_fpos, null == m_field ? 0L : m_field.getNonVolatileHandler());
	    break;
	}

    }

    /**
     * get the value of this field
     *
     * @return the field value
     */
    @SuppressWarnings("unchecked")
    public E get() {
	E ret = null;
	switch (m_dgftype) {
	case BYTE:
	    ret = (E) Byte.valueOf(m_unsafe.getByte(m_fpos));
	    break;
	case BOOLEAN:
	    ret = (E) Boolean.valueOf(1 == m_unsafe.getByte(m_fpos));
	    break;
	case CHARACTER:
	    ret = (E) Character.valueOf(m_unsafe.getChar(m_fpos));
	    break;
	case SHORT:
	    ret = (E) Short.valueOf(m_unsafe.getShort(m_fpos));
	    break;
	case INTEGER:
	    ret = (E) Integer.valueOf(m_unsafe.getInt(m_fpos));
	    break;
	case LONG:
	    ret = (E) Long.valueOf(m_unsafe.getLong(m_fpos));
	    break;
	case FLOAT:
	    ret = (E) Float.valueOf(m_unsafe.getFloat(m_fpos));
	    break;
	case DOUBLE:
	    ret = (E) Double.valueOf(m_unsafe.getDouble(m_fpos));
	    break;
	case STRING:
	    if (null == m_strfield) {
		long phandler = m_unsafe.getAddress(m_fpos);
		if (0L != phandler) {
		    m_strfield = m_allocator.retrieveBuffer(phandler, m_autoreclaim);
		    if (null == m_strfield) {
			throw new RetrieveNonVolatileEntityError("Retrieve String Buffer Failure.");
		    }
		}
	    }
	    ret = (E) (null == m_strfield ? null : m_strfield.get().asCharBuffer().toString());
	    break;
	case DURABLE:
	    if (null == m_field) {
		long phandler = m_unsafe.getAddress(m_fpos);
		if (0L != phandler) {
		    if (null == m_defproxy) {
			throw new RetrieveNonVolatileEntityError("Proxy not specified for Non-Volatile Generic entity.");
		    }
		    m_field = m_defproxy.restore(m_allocator, m_efproxies, m_gftypes, phandler, m_autoreclaim);
		}
	    }
	    ret = (E) m_field;
	    break;
	}
	return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeAfterCreate() {
	throw new UnsupportedOperationException("GenericField.initializeAfterCreate()");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeAfterRestore() {
	throw new UnsupportedOperationException("GenericField.initializeAfterRestore()");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelAutoReclaim() {
	if (null != m_field) {
	    m_field.cancelAutoReclaim();
	}
	if (null != m_strfield) {
	    m_strfield.cancelAutoReclaim();
	}		
	m_autoreclaim = false;		
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerAutoReclaim() {
	if (null != m_field) {
	    m_field.registerAutoReclaim();
	}
	if (null != m_strfield) {
	    m_strfield.registerAutoReclaim();
	}		
	m_autoreclaim = true;		
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getNonVolatileHandler() {
	throw new UnsupportedOperationException("GenericField.getNonVolatileHandler()");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean autoReclaim() {
	return m_autoreclaim;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() throws RetrieveNonVolatileEntityError {
	if (null != m_field) {
	    m_field.destroy();
	}
	if (null != m_strfield) {
	    m_strfield.destroy();
	}		
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setupGenericInfo(EntityFactoryProxy[] efproxies, GType[] gftypes) {
	throw new UnsupportedOperationException("GenericField.setupGenericInfo()");
    }
}
