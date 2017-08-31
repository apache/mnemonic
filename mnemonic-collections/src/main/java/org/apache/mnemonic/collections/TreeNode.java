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
import org.apache.mnemonic.DurableEntity;
import org.apache.mnemonic.DurableGetter;
import org.apache.mnemonic.DurableSetter;
import org.apache.mnemonic.RetrieveDurableEntityError;

/**
 * this class defines a non-volatile node for a generic value in a tree structure
 *
 */
@DurableEntity
public abstract class TreeNode<E extends Comparable<E>> implements Durable, Comparable<TreeNode<E>> {
  protected transient EntityFactoryProxy[] m_node_efproxies;
  protected transient DurableType[] m_node_gftypes;

  /**
   * creation callback for initialization
   *
   */
  @Override
  public void initializeAfterCreate() {
    // System.out.println("Initializing After Created");
  }

  /**
   * restore callback for initialization
   *
   */
  @Override
  public void initializeAfterRestore() {
    // System.out.println("Initializing After Restored");
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
   * get item field of this node
   *
   * @return the item field of this node
   *
   */
  @DurableGetter(Id = 1L, EntityFactoryProxies = "m_node_efproxies", GenericFieldTypes = "m_node_gftypes")
  public abstract E getItem();

  /**
   * set item in the node
   *
   * @param item
   *          the item to be set
   *
   * @param destroy
   *          true if want to destroy the exist node
   */
  @DurableSetter
  public abstract void setItem(E item, boolean destroy);

  /**
   * get left pointer of this node
   *
   * @return the left pointer of this node
   *
   */
  @DurableGetter(Id = 2L, EntityFactoryProxies = "m_node_efproxies", GenericFieldTypes = "m_node_gftypes")
  public abstract TreeNode<E> getLeft();

  /**
   * set left pointer in the node
   *
   * @param left
   *          the left pointer to be set
   *
   * @param destroy
   *          true if want to destroy the exist node
   */
  @DurableSetter
  public abstract void setLeft(TreeNode<E> left, boolean destroy);

  /**
   * get right pointer of this node
   *
   * @return the right pointer of this node
   *
   */
  @DurableGetter(Id = 3L, EntityFactoryProxies = "m_node_efproxies", GenericFieldTypes = "m_node_gftypes")
  public abstract TreeNode<E> getRight();

  /**
   * set right pointer in the node
   *
   * @param right
   *          the right pointer to be set
   *
   * @param destroy
   *          true if want to destroy the exist node
   */
  @DurableSetter
  public abstract void setRight(TreeNode<E> right, boolean destroy);

  /**
   * get parent of this node
   *
   * @return the parent pointer of this node
   *
   */
  @DurableGetter(Id = 4L, EntityFactoryProxies = "m_node_efproxies", GenericFieldTypes = "m_node_gftypes")
  public abstract TreeNode<E> getParent();

  /**
   * set parent in the node
   *
   * @param parent
   *          the parent pointer to be set
   *
   * @param destroy
   *          true if want to destroy the exist node
   */
  @DurableSetter
  public abstract void setParent(TreeNode<E> parent, boolean destroy);


  /**
   * get color of this node 1-red, 0-black
   *
   * @return the color of this node
   *
   */
  @DurableGetter(Id = 5L, EntityFactoryProxies = "m_node_efproxies", GenericFieldTypes = "m_node_gftypes")
  public abstract boolean getColor();

  /**
   * set color in the node 1-red, 0-black
   *
   * @param color
   *          the color to be set
   */
  @DurableSetter
  public abstract void setColor(boolean color);

  @Override
  public int compareTo(TreeNode<E> other) {
    return getItem().compareTo(other.getItem());
  }

  public boolean equals(TreeNode<E> other) {
    if (null == other) {
      return false;
    } else {
      return getHandler() == other.getHandler();
    }
  }

  public int hashCode() {
    return getItem().hashCode();
  }

  public int compareTo(E item) {
    return getItem().compareTo(item);
  }

  public void testOutput() throws RetrieveDurableEntityError {
    System.out.println(((getColor() == false) ? "Color: Red " : "Color: Black ")
                         + "Item: " + getItem()
                         + " Current: " + getHandler()
                         + " Left: " + ((getLeft() == null) ? "NULL" : getLeft().getHandler())
                         + " Right: " + ((getRight() == null) ? "NULL" : getRight().getHandler())
                         + " Parent: " + ((getParent() == null) ? "NULL" : getParent().getHandler()));
   }
}
