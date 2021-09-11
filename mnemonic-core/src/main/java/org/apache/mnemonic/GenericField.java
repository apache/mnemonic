/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mnemonic;

import org.apache.mnemonic.resgc.ReclaimContext;

/**
 * this class defines generic field for non-volatile entity
 *
 */

@SuppressWarnings("restriction")
public class GenericField<A extends RestorableAllocator<A>, E> implements Durable {

  @SuppressWarnings({"restriction", "UseOfSunClasses"})
  private sun.misc.Unsafe m_unsafe;
  private long m_fpos;
  private DurableType m_dgftype = null;
  private Durable m_field = null;
  private DurableBuffer<A> m_strfield = null;
  private DurableChunk<A> m_chunkfield = null;
  private DurableBuffer<A> m_bufferfield = null;
  private A m_allocator;
  private Persistence<A> m_persistOps = null;
  private boolean m_autoreclaim;
  private ReclaimContext m_reclaimcontext = null;
  private EntityFactoryProxy m_defproxy = null;
  private EntityFactoryProxy[] m_efproxies;
  private DurableType[] m_gftypes;

  /**
   * Constructor: initialize this generic field
   *
   * @param defproxy
   *          specify its entity factory proxy
   *
   * @param dgftype
   *          specify its type
   *
   * @param efproxies
   *          specify an array of containing entity factory proxies
   *
   * @param gftypes
   *          specify an array of containing types corresponding to dfproxies
   *
   * @param allocator
   *          specify the allocator this field sit on
   *
   * @param unsafe
   *          specify the unsafe instance
   *
   * @param autoreclaim
   *          specify true if ask for auto-reclaim for this field
   *
   * @param fpos
   *          specify the field position
   *
   */
  @SuppressWarnings({"restriction", "UseOfSunClasses", "unchecked"})
  public GenericField(EntityFactoryProxy defproxy, DurableType dgftype,
      EntityFactoryProxy[] efproxies, DurableType[] gftypes,
      A allocator, sun.misc.Unsafe unsafe, boolean autoreclaim, ReclaimContext rctx, Long fpos) {
    m_unsafe = unsafe;
    m_fpos = fpos;
    m_allocator = allocator;
    m_autoreclaim = autoreclaim;
    m_reclaimcontext = rctx;
    m_efproxies = efproxies;
    m_gftypes = gftypes;
    m_defproxy = defproxy;
    m_dgftype = dgftype;
    if (m_allocator instanceof Persistence) {
      m_persistOps = (Persistence<A>) m_allocator;
    }
  }

  /**
   * set a value to this field
   *
   * @param e
   *          specify a value to set
   *
   * @param destroy
   *          specify true if want to destroy the original value
   */
  @SuppressWarnings("unchecked")
  public void set(E e, boolean destroy) {
    boolean isnull = null == e;
    switch (m_dgftype) {
    case BYTE:
      m_unsafe.putByte(m_fpos, isnull ? (byte) 0 : (Byte) e);
      break;
    case BOOLEAN:
      m_unsafe.putByte(m_fpos, isnull ? (byte) 0 : ((Boolean) e ? (byte) 1 : (byte) 0));
      break;
    case CHARACTER:
      m_unsafe.putChar(m_fpos, isnull ? (char) 0 : (Character) e);
      break;
    case SHORT:
      m_unsafe.putShort(m_fpos, isnull ? (short) 0 : (Short) e);
      break;
    case INTEGER:
      m_unsafe.putInt(m_fpos, isnull ? (int) 0 : (Integer) e);
      break;
    case LONG:
      m_unsafe.putLong(m_fpos, isnull ? (long) 0 : (Long) e);
      break;
    case FLOAT:
      m_unsafe.putFloat(m_fpos, isnull ? (float) 0 : (Float) e);
      break;
    case DOUBLE:
      m_unsafe.putDouble(m_fpos, isnull ? (double) 0 : (Double) e);
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
        m_strfield = m_allocator.createBuffer(str.length() * 2,
                m_autoreclaim, m_reclaimcontext);
        if (null == m_strfield) {
          throw new OutOfHybridMemory("Create Durable String Error!");
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
      if (null != e) {
        m_field = (Durable) e;
        if (m_autoreclaim) {
          m_field.registerAutoReclaim();
        } else {
          m_field.cancelAutoReclaim();
        }
        m_unsafe.putAddress(m_fpos, null == m_field ? 0L : m_field.getHandler());
      }
      break;
    case CHUNK:
      if (destroy && null != get()) {
        m_chunkfield.destroy();
        m_chunkfield = null;
        m_unsafe.putAddress(m_fpos, 0L);
      }
      if (null != e) {
        if (e instanceof DurableChunk<?> == false) {
          throw new GenericTypeError("generic type is not mapped to a chunk type!");
        }
        if (((DurableChunk<A>) e).getAllocator() != m_allocator) {
          throw new IllegalAllocatorError("This generic chunk is allocated by another allocator!");
        }
        m_chunkfield = (DurableChunk<A>) e;
        if (m_autoreclaim) {
          m_chunkfield.registerAutoReclaim();
        } else {
          m_chunkfield.cancelAutoReclaim();
        }
        m_unsafe.putAddress(m_fpos, null == m_chunkfield ? 0L : m_chunkfield.getHandler());
      }
      break;
    case BUFFER :
      if (destroy && null != get()) {
        m_bufferfield.destroy();
        m_bufferfield = null;
        m_unsafe.putAddress(m_fpos, 0L);
      }
      if (null != e) {
        if (e instanceof DurableBuffer<?> == false) {
          throw new GenericTypeError("generic type is not mapped to a buffer type!");
        }
        if (((DurableBuffer<A>) e).getAllocator() != m_allocator) {
          throw new IllegalAllocatorError("This generic buffer is allocated by another allocator!");
        }
        m_bufferfield = (DurableBuffer<A>) e;
        if (m_autoreclaim) {
          m_bufferfield.registerAutoReclaim();
        } else {
          m_bufferfield.cancelAutoReclaim();
        }
        m_unsafe.putAddress(m_fpos, null == m_bufferfield ? 0L : m_bufferfield.getHandler());
      }
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
            throw new RetrieveDurableEntityError("Retrieve String Buffer Failure.");
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
            throw new RetrieveDurableEntityError("Proxy not specified for Non-Volatile Generic entity.");
          }
          m_field = m_defproxy.restore(m_allocator, m_efproxies, m_gftypes, phandler, m_autoreclaim);
        }
      }
      ret = (E) m_field;
      break;
    case CHUNK:
      if (null == m_chunkfield) {
        long phandler = m_unsafe.getAddress(m_fpos);
        if (0L != phandler) {
          m_chunkfield = m_allocator.retrieveChunk(phandler, m_autoreclaim);
          if (null == m_chunkfield) {
            throw new RetrieveDurableEntityError("Retrieve Chunk Failure.");
          }
        }
      }
      ret = (E) m_chunkfield;
      break;
    case BUFFER:
      if (null == m_bufferfield) {
        long phandler = m_unsafe.getAddress(m_fpos);
        if (0L != phandler) {
          m_bufferfield = m_allocator.retrieveBuffer(phandler, m_autoreclaim);
          if (null == m_bufferfield) {
            throw new RetrieveDurableEntityError("Retrieve Buffer Failure.");
          }
        }
      }
      ret = (E) m_bufferfield;
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
    if (null != m_chunkfield) {
      m_chunkfield.cancelAutoReclaim();
    }
    if (null != m_bufferfield) {
      m_bufferfield.cancelAutoReclaim();
    }
    m_autoreclaim = false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerAutoReclaim() {
    registerAutoReclaim(null);
  }

  @Override
  public void registerAutoReclaim(ReclaimContext rctx) {
    if (null != m_field) {
      m_field.registerAutoReclaim(rctx);
    }
    if (null != m_strfield) {
      m_strfield.registerAutoReclaim(rctx);
    }
    if (null != m_chunkfield) {
      m_chunkfield.registerAutoReclaim(rctx);
    }
    if (null != m_bufferfield) {
      m_bufferfield.registerAutoReclaim(rctx);
    }
    m_autoreclaim = true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getHandler() {
    throw new UnsupportedOperationException("GenericField.getHandler()");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean autoReclaim() {
    return m_autoreclaim;
  }

  /**
   * sync. this generic field
   */
  @Override
  public void syncToVolatileMemory() {
    if (null != m_allocator) {
      switch (m_dgftype) {
        case BYTE:
          m_allocator.syncToVolatileMemory(m_fpos, Byte.BYTES, false);
          break;
        case BOOLEAN:
          m_allocator.syncToVolatileMemory(m_fpos, Byte.BYTES, false);
          break;
        case CHARACTER:
          m_allocator.syncToVolatileMemory(m_fpos, Character.BYTES, false);
          break;
        case SHORT:
          m_allocator.syncToVolatileMemory(m_fpos, Short.BYTES, false);
          break;
        case INTEGER:
          m_allocator.syncToVolatileMemory(m_fpos, Integer.BYTES, false);
          break;
        case LONG:
          m_allocator.syncToVolatileMemory(m_fpos, Long.BYTES, false);
          break;
        case FLOAT:
          m_allocator.syncToVolatileMemory(m_fpos, Float.BYTES, false);
          break;
        case DOUBLE:
          m_allocator.syncToVolatileMemory(m_fpos, Double.BYTES, false);
          break;
        case STRING:
          if (null != m_strfield) {
            m_strfield.syncToVolatileMemory();
          }
          break;
        case DURABLE:
          if (null != m_field) {
            m_field.syncToVolatileMemory();
          }
          break;
        case CHUNK:
          if (null != m_chunkfield) {
            m_chunkfield.syncToVolatileMemory();
          }
          break;
        case BUFFER:
          if (null != m_bufferfield) {
            m_bufferfield.syncToVolatileMemory();
          }
          break;
      }
    }
  }

  /**
   * Make any cached changes to this generic field persistent.
   */
  @Override
  public void syncToNonVolatileMemory() {
    if (null != m_persistOps) {
      switch (m_dgftype) {
        case BYTE:
          m_persistOps.syncToNonVolatileMemory(m_fpos, Byte.BYTES, false);
          break;
        case BOOLEAN:
          m_persistOps.syncToNonVolatileMemory(m_fpos, Byte.BYTES, false);
          break;
        case CHARACTER:
          m_persistOps.syncToNonVolatileMemory(m_fpos, Character.BYTES, false);
          break;
        case SHORT:
          m_persistOps.syncToNonVolatileMemory(m_fpos, Short.BYTES, false);
          break;
        case INTEGER:
          m_persistOps.syncToNonVolatileMemory(m_fpos, Integer.BYTES, false);
          break;
        case LONG:
          m_persistOps.syncToNonVolatileMemory(m_fpos, Long.BYTES, false);
          break;
        case FLOAT:
          m_persistOps.syncToNonVolatileMemory(m_fpos, Float.BYTES, false);
          break;
        case DOUBLE:
          m_persistOps.syncToNonVolatileMemory(m_fpos, Double.BYTES, false);
          break;
        case STRING:
          if (null != m_strfield) {
            m_strfield.syncToNonVolatileMemory();
          }
          break;
        case DURABLE:
          if (null != m_field) {
            m_field.syncToNonVolatileMemory();
          }
          break;
        case CHUNK:
          if (null != m_chunkfield) {
            m_chunkfield.syncToNonVolatileMemory();
          }
          break;
        case BUFFER:
          if (null != m_bufferfield) {
            m_bufferfield.syncToNonVolatileMemory();
          }
          break;
      }
    }
  }

  /**
   * flush processors cache for this generic field
   */
  @Override
  public void syncToLocal() {
    if (null != m_persistOps) {
      switch (m_dgftype) {
        case BYTE:
          m_persistOps.syncToLocal(m_fpos, Byte.BYTES, false);
          break;
        case BOOLEAN:
          m_persistOps.syncToLocal(m_fpos, Byte.BYTES, false);
          break;
        case CHARACTER:
          m_persistOps.syncToLocal(m_fpos, Character.BYTES, false);
          break;
        case SHORT:
          m_persistOps.syncToLocal(m_fpos, Short.BYTES, false);
          break;
        case INTEGER:
          m_persistOps.syncToLocal(m_fpos, Integer.BYTES, false);
          break;
        case LONG:
          m_persistOps.syncToLocal(m_fpos, Long.BYTES, false);
          break;
        case FLOAT:
          m_persistOps.syncToLocal(m_fpos, Float.BYTES, false);
          break;
        case DOUBLE:
          m_persistOps.syncToLocal(m_fpos, Double.BYTES, false);
          break;
        case STRING:
          if (null != m_strfield) {
            m_strfield.syncToLocal();
          }
          break;
        case DURABLE:
          if (null != m_field) {
            m_field.syncToLocal();
          }
          break;
        case CHUNK:
          if (null != m_chunkfield) {
            m_chunkfield.syncToLocal();
          }
          break;
        case BUFFER:
          if (null != m_bufferfield) {
            m_bufferfield.syncToLocal();
          }
          break;
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void destroy() throws RetrieveDurableEntityError {
    if (null != get()) {
      if (null != m_field) {
        m_field.destroy();
      }
      if (null != m_strfield) {
        m_strfield.destroy();
      }
      if (null != m_chunkfield) {
        m_chunkfield.destroy();
      }
      if (null != m_bufferfield) {
        m_bufferfield.destroy();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setupGenericInfo(EntityFactoryProxy[] efproxies, DurableType[] gftypes) {
    throw new UnsupportedOperationException("GenericField.setupGenericInfo()");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long[][] getNativeFieldInfo() {
    throw new UnsupportedOperationException("getNativeFieldInfo() on Generic Field.");
  }

  @Override
  public void refbreak() {
    m_field = null;
    m_strfield = null;
    m_chunkfield = null;
    m_bufferfield = null;
  }
}
