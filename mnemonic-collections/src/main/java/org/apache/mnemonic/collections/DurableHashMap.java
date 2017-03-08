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

package org.apache.mnemonic.collections;

import org.apache.mnemonic.Durable;
import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.DurableType;

import java.util.Iterator;

public abstract class DurableHashMap<K, V> implements Durable, Iterable<MapEntry<K, V>> {
  protected transient EntityFactoryProxy[] m_node_efproxies;
  protected transient DurableType[] m_node_gftypes;
  protected long threshold;
  protected long totalCapacity;
  protected long mapSize = 0;
  
  /**
   * creation callback for initialization
   *
   */
  @Override
  public void initializeAfterCreate() {
  //   System.out.println("Initializing After Created");
  }

  /**
   * restore callback for initialization
   *
   */
  @Override
  public void initializeAfterRestore() {
  //   System.out.println("Initializing After Restored");
  }

  /**
   * this function will be invoked by its factory to setup generic related info
   * to avoid expensive operations from reflection
   *
   * @param efproxies
   *          specify a array of factory to proxy the restoring of its generic
   *          field objects
   *
   * @param gftypes
   *          specify a array of types corresponding to efproxies
   */
  @Override
  public void setupGenericInfo(EntityFactoryProxy[] efproxies, DurableType[] gftypes) {
    m_node_efproxies = efproxies;
    m_node_gftypes = gftypes;
  }
 
  /**
   * Add a new key-value pair to map
   * 
   * @param key
   *          the key to be set
   *
   * @param value
   *          the value to be set
   *
   * @return previous value with key else return null
   */
  public abstract V put(K key, V value);

  /**
   * Return a value to which key is mapped
   * 
   * @param key
   *          the key whose value is to be retrieved
   *
   * @return previous value with key else return null
   */
  public abstract V get(K key);

  /**
   * Remove a mapping for a specified key
   * 
   * @param key
   *          the key whose value is to be removed
   *
   * @return previous value with key else return null
   */
  public abstract V remove(K key);

  /**
   * Retrieve a iterator
   *
   * @return iterator to the hash map
   */

  public abstract Iterator<MapEntry<K, V>> iterator();
  /**
   * Apply hash function to given hash code
   * 
   * @param hashcode
   *          the hashcode of the object
   *
   * @return result of the hash function 
   */
  public int hash(int hashcode) {
    hashcode ^= (hashcode >>> 20) ^ (hashcode >>> 12);
    return hashcode ^ (hashcode >>> 7) ^ (hashcode >>> 4);
  }

  /**
   * Map the hash to a bucket
   * 
   * @param hash
   *          the hashcode of the object
   *
   * @return index of the bucket
   */
  public long getBucketIndex(int hash) {
    return hash & (totalCapacity - 1);
  }
  /**
   * Get the number of elements in the map
   * 
   * @return size of the map
   */
  public long getSize() {
    return mapSize;
  }

}
    
