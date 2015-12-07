package com.intel.bigdatamem;

/**
 *
 * @author Wang, Gang {@literal <gang1.wang@intel.com>}
 *
 */


import sun.misc.Unsafe;

@SuppressWarnings("restriction")
public class GenericField<A extends CommonPersistAllocator<A>, E> implements Durable {
	public enum GType {
		BOOLEAN, CHARACTER, BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, STRING, DURABLE
	};

	private Unsafe m_unsafe;
	private long m_fpos;
	private GType m_dgftype = null;
	private Durable m_field = null;
	private MemBufferHolder<A> m_strfield = null;
	private A m_allocater;
	private boolean m_autoreclaim;
	private EntityFactoryProxy m_defproxy = null;
	private EntityFactoryProxy[] m_efproxies;
	private GType[] m_gftypes;

	public GenericField(EntityFactoryProxy defproxy, GType dgftype, EntityFactoryProxy[] efproxies, GType[] gftypes, 
			A allocater, Unsafe unsafe, boolean autoreclaim, Long fpos) {
		m_unsafe = unsafe;
		m_fpos = fpos;
		m_allocater = allocater;
		m_autoreclaim = autoreclaim;
		m_efproxies = efproxies;
		m_gftypes = gftypes;
		m_defproxy = defproxy;
		m_dgftype = dgftype;
	}

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
				m_strfield = m_allocater.createBuffer(str.length() * 2, m_autoreclaim);
				if (null == m_strfield) {
					throw new OutOfPersistentMemory("Create Persistent String Error!");
				}
				m_strfield.get().asCharBuffer().put(str);
				m_unsafe.putAddress(m_fpos, m_allocater.getBufferHandler(m_strfield));
			}
			break;
		case DURABLE:
			if (destroy && null != get()) {
				m_field.destroy();
				m_field = null;
				m_unsafe.putAddress(m_fpos, 0L);
			}
			m_field = (Durable) e;
			m_unsafe.putAddress(m_fpos, null == m_field ? 0L : m_field.getPersistentHandler());
			break;
		}

	}

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
					m_strfield = m_allocater.retrieveBuffer(phandler, m_autoreclaim);
					if (null == m_strfield) {
						throw new RetrievePersistentEntityError("Retrieve String Buffer Failure.");
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
						throw new RetrievePersistentEntityError("Proxy not specified for Durable Generic entity.");
					}
					m_field = m_defproxy.restore(m_allocater, m_efproxies, m_gftypes, phandler, m_autoreclaim);
				}
			}
			ret = (E) m_field;
			break;
		}
		return ret;
	}

	@Override
	public void initializeAfterCreate() {
		throw new UnsupportedOperationException("GenericField.initializeAfterCreate()");
	}

	@Override
	public void initializeAfterRestore() {
		throw new UnsupportedOperationException("GenericField.initializeAfterRestore()");
	}

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

	@Override
	public long getPersistentHandler() {
		throw new UnsupportedOperationException("GenericField.getPersistentHandler()");
	}

	@Override
	public boolean autoReclaim() {
		return m_autoreclaim;
	}

	@Override
	public void destroy() throws RetrievePersistentEntityError {
		if (null != m_field) {
			m_field.destroy();
		}
		if (null != m_strfield) {
			m_strfield.destroy();
		}		
	}

	@Override
	public void setupGenericInfo(EntityFactoryProxy[] efproxies, GType[] gftypes) {
		throw new UnsupportedOperationException("GenericField.setupGenericInfo()");
	}
}
