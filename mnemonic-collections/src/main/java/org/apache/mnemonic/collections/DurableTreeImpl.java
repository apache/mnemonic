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

import org.apache.mnemonic.EntityFactoryProxy;
import org.apache.mnemonic.DurableType;
import org.apache.mnemonic.MemChunkHolder;
import org.apache.mnemonic.MemoryDurableEntity;
import org.apache.mnemonic.OutOfHybridMemory;
import org.apache.mnemonic.RestorableAllocator;
import org.apache.mnemonic.RestoreDurableEntityError;
import org.apache.mnemonic.resgc.ReclaimContext;
import org.apache.mnemonic.RetrieveDurableEntityError;
import org.apache.mnemonic.Utils;

@SuppressWarnings({"restriction", "unchecked"})
public class DurableTreeImpl<A extends RestorableAllocator<A>, E extends Comparable<E>>
  extends DurableTree<E> implements MemoryDurableEntity<A> {

  private static long[][] fieldInfo;
  @SuppressWarnings({"restriction", "UseOfSunClasses"})
  private sun.misc.Unsafe unsafe;
  private EntityFactoryProxy[] factoryProxy;
  private DurableType[] genericType;
  private volatile boolean autoReclaim;
  private volatile ReclaimContext reclaimcontext;
  private A allocator;
  private MemChunkHolder<A> holder;
  private static final long MAX_OBJECT_SIZE = 8;
  private TreeNode<E> root;
  private static final boolean RED = false;
  private static final boolean BLACK = true;

  /**
   * adds a specific element to the tree
   *
   * @param item
   *          the item to be added
   */
  public void insert(E item) throws OutOfHybridMemory {
    TreeNode<E> tmp = root;
    TreeNode<E> node = TreeNodeFactory.create(allocator, factoryProxy, genericType, autoReclaim, reclaimcontext);
    node.setItem(item, false);
    if (null == root) {
      root = node;
      unsafe.putLong(holder.get(), root.getHandler());
      node.setColor(BLACK);
    } else {
      node.setColor(RED);
      while (true) {
        int ret = node.compareTo(tmp);
        if (ret < 0) {
          if (null == tmp.getLeft()) {
            tmp.setLeft(node, false);
            node.setParent(tmp, false);
            break;
          } else {
            tmp = tmp.getLeft();
          }
        } else if (ret >= 0) {
          if (null == tmp.getRight()) {
            tmp.setRight(node, false);
            node.setParent(tmp, false);
            break;
          } else {
            tmp = tmp.getRight();
          }
        }
      }
      fixInsert(node);
    }
  }

  protected void fixInsert(TreeNode<E> currentNode) {
    while (currentNode.getParent() != null && currentNode.getParent().getColor() == RED) {
      TreeNode<E> u = null;
      if ((currentNode.getParent()).equals(currentNode.getParent().getParent().getLeft())) {
        u = currentNode.getParent().getParent().getRight();

        if (u != null && u.getColor() == RED) {
          currentNode.getParent().setColor(BLACK);
          u.setColor(BLACK);
          currentNode.getParent().getParent().setColor(RED);
          currentNode = currentNode.getParent().getParent();
          continue;
        }
        if (currentNode.equals(currentNode.getParent().getRight())) {
          currentNode = currentNode.getParent();
          rotateLeft(currentNode);
        }
        currentNode.getParent().setColor(BLACK);
        currentNode.getParent().getParent().setColor(RED);
        rotateRight(currentNode.getParent().getParent());
      } else {
        u = currentNode.getParent().getParent().getLeft();
        if (u != null && u.getColor() == RED) {
          currentNode.getParent().setColor(BLACK);
          u.setColor(BLACK);
          currentNode.getParent().getParent().setColor(RED);
          currentNode = currentNode.getParent().getParent();
          continue;
        }
        if (currentNode.equals(currentNode.getParent().getLeft())) {
          currentNode = currentNode.getParent();
          rotateRight(currentNode);
        }
        currentNode.getParent().setColor(BLACK);
        currentNode.getParent().getParent().setColor(RED);
        rotateLeft(currentNode.getParent().getParent());
      }
    }
    root.setColor(BLACK);
  }

  protected void rotateLeft(TreeNode<E> currentNode) {
    if (currentNode.getParent() != null) {
      if (currentNode.equals(currentNode.getParent().getLeft())) {
        currentNode.getParent().setLeft(currentNode.getRight(), false);
      } else {
        currentNode.getParent().setRight(currentNode.getRight(), false);
      }
      currentNode.getRight().setParent(currentNode.getParent(), false);
      currentNode.setParent(currentNode.getRight(), false);
      if (currentNode.getRight().getLeft() != null) {
        currentNode.getRight().getLeft().setParent(currentNode, false);
      }
      currentNode.setRight(currentNode.getRight().getLeft(), false);
      currentNode.getParent().setLeft(currentNode, false);
    } else {
      TreeNode<E> right = root.getRight();
      root.setRight(right.getLeft(), false);
      if (right.getLeft() != null) {
        right.getLeft().setParent(root, false);
      }
      root.setParent(right, false);
      right.setLeft(root, false);
      right.setParent(null, false);
      root = right;
      unsafe.putLong(holder.get(), root.getHandler());
    }
  }

  protected void rotateRight(TreeNode<E> currentNode) {
    if (currentNode.getParent() != null) {
      if (currentNode.equals(currentNode.getParent().getLeft())) {
        currentNode.getParent().setLeft(currentNode.getLeft(), false);
      } else {
        currentNode.getParent().setRight(currentNode.getLeft(), false);
      }

      currentNode.getLeft().setParent(currentNode.getParent(), false);
      currentNode.setParent(currentNode.getLeft(), false);
      if (currentNode.getLeft().getRight() != null) {
        currentNode.getLeft().getRight().setParent(currentNode, false);
      }
      currentNode.setLeft(currentNode.getLeft().getRight(), false);
      currentNode.getParent().setRight(currentNode, false);
    } else {
      TreeNode<E> left = root.getLeft();
      root.setLeft(root.getLeft().getRight(), false);
      if (left.getRight() != null) {
        left.getRight().setParent(root, false);
      }
      root.setParent(left, false);
      left.setRight(root, false);
      left.setParent(null, false);
      root = left;
      unsafe.putLong(holder.get(), root.getHandler());
    }
  }

  /**
   * removes a specific element from the tree
   *
   * @param item
   *          the item to be removed
   *
   * @return true if element is removed
   */
  public boolean remove(E item, boolean destroy) {
    TreeNode<E> currentNode = findNode(item);
    if (null == currentNode) {
      return false;
    }
    TreeNode<E> tmp, ref = currentNode;
    boolean col = ref.getColor();
    if (null == currentNode.getLeft()) {
      tmp = currentNode.getRight();
      replace(currentNode, currentNode.getRight());
    } else if (null == currentNode.getRight()) {
      tmp = currentNode.getLeft();
      replace(currentNode, currentNode.getLeft());
    } else {
      ref = findMinimum(currentNode.getRight());
      col = ref.getColor();
      tmp = ref.getRight();
      if (ref.getParent().equals(currentNode)) {
        tmp.setParent(ref, false);
      } else {
        replace(ref, ref.getRight());
        ref.setRight(currentNode.getRight(), false);
        ref.getRight().setParent(ref, false);
      }
      replace(currentNode, ref);
      ref.setLeft(currentNode.getLeft(), false);
      ref.getLeft().setParent(ref, false);
      ref.setColor(currentNode.getColor());
    }
    if (col == BLACK && tmp != null) {
      fixDelete(tmp);
    }
    if (destroy) {
      currentNode.setLeft(null, false);
      currentNode.setRight(null, false);
      currentNode.setParent(null, false);
      currentNode.destroy();
    }
    return true;
  }

  protected void fixDelete(TreeNode<E> currentNode) {
    while (!currentNode.equals(root) && currentNode.getColor() == BLACK) {
      if (currentNode.equals(currentNode.getParent().getLeft())) {
        TreeNode<E> other = currentNode.getParent().getRight();
        if (other.getColor() == RED) {
          other.setColor(BLACK);
          currentNode.getParent().setColor(RED);
          rotateLeft(currentNode.getParent());
          other = currentNode.getParent().getRight();
        }
        if (other.getLeft().getColor() == BLACK && other.getRight().getColor() == BLACK) {
          other.setColor(RED);
          currentNode = currentNode.getParent();
          continue;
        } else if (other.getRight().getColor() == BLACK) {
          other.getLeft().setColor(BLACK);
          other.setColor(RED);
          rotateRight(other);
          other = currentNode.getParent().getRight();
        }
        if (other.getRight().getColor() == RED) {
          other.setColor(currentNode.getParent().getColor());
          currentNode.getParent().setColor(BLACK);
          other.getRight().setColor(BLACK);
          rotateLeft(currentNode.getParent());
          currentNode = root;
        }
      } else {
        TreeNode other = currentNode.getParent().getLeft();
        if (other.getColor() == RED) {
          other.setColor(BLACK);
          currentNode.getParent().setColor(RED);
          rotateRight(currentNode.getParent());
          other = currentNode.getParent().getLeft();
        }
        if (other.getRight().getColor() == BLACK && other.getLeft().getColor() == BLACK) {
          other.setColor(RED);
          currentNode = currentNode.getParent();
          continue;
        } else if (other.getLeft().getColor() == BLACK) {
          other.getRight().setColor(BLACK);
          other.setColor(RED);
          rotateLeft(other);
          other = currentNode.getParent().getLeft();
        }
        if (other.getLeft().getColor() == RED) {
          other.setColor(currentNode.getParent().getColor());
          currentNode.getParent().setColor(BLACK);
          other.getLeft().setColor(BLACK);
          rotateRight(currentNode.getParent());
          currentNode = root;
        }
      }
    }
    currentNode.setColor(BLACK);
  }

  protected void replace(TreeNode<E> first, TreeNode<E> second) {
    if (null == first.getParent()) {
      root = second;
      unsafe.putLong(holder.get(), root.getHandler());
    } else if (first.equals(first.getParent().getLeft())) {
      first.getParent().setLeft(second, false);
    } else {
      first.getParent().setRight(second, false);
    }
    if (second != null) {
      second.setParent(first.getParent(), false);
    }
  }

  /**
   * checks if tree contains the specified element
   *
   * @param item
   *          the item to be set
   *
   * @return true if tree contains the element
   */
  public boolean contains(E item) {
    return findNode(item) == null ? false : true;
  }

  protected TreeNode<E> findNode(E item) {
    return findNode(item, root);
  }

  protected TreeNode<E> findNode(E item, TreeNode node) {
    if (null == node) {
      return null;
    }
    int ret = node.compareTo(item);
    if (ret < 0) {
      return findNode(item, node.getRight());
    } else if (ret > 0) {
      return findNode(item, node.getLeft());
    } else if (ret == 0) {
      return node;
    }
    return null;
  }

  /**
   * print tree
   *
   */
  public void print() {
    print(root);
  }

  protected void print(TreeNode<E> node) {
    if (null == node) {
      return;
    }
    print(node.getLeft());
    node.testOutput();
    print(node.getRight());
  }

  /**
   * find successor item
   *
   * @param item
   *          the item in the traversal
   *
   * @return successor item in the inorder traversal
   */
  public E successor(E item) {
    return successor(item, findNode(item));
  }

  protected E successor(E item, TreeNode<E> node) {
    if (null == node) {
      return null;
    }
    if (node.getRight() != null) {
      return findMinimum(node.getRight()).getItem();
    }

    TreeNode<E> parent = node.getParent();
    if (null == parent) {
      return null;
    }
    TreeNode currentNode = node;
    while (parent != null && currentNode.equals(parent.getRight())) {
      currentNode = parent;
      parent = currentNode.getParent();
    }
    if (null == parent) {
      return null;
    } else {
      return parent.getItem();
    }
  }

  protected TreeNode<E> findMinimum(TreeNode<E> node) {
    TreeNode<E> current = node;
    while (current.getLeft() != null) {
      current = current.getLeft();
    }
    return current;
  }

  /**
   * find predecessor item
   *
   * @param item
   *          the item in the traversal
   *
   * @return predecessor item in the inorder traversal
   */
  public E predecessor(E item) {
    return predecessor(item, findNode(item));
  }

  protected E predecessor(E item, TreeNode<E> node) {
    if (null == node) {
      return null;
    }
    if (node.getLeft() != null) {
      return findMaximum(node.getLeft()).getItem();
    }

    TreeNode<E> parent = node.getParent();

    TreeNode currentNode = node;
    while (parent != null && currentNode.equals(parent.getLeft())) {
      currentNode = parent;
      parent = currentNode.getParent();
    }
    if (null == parent) {
      return null;
    } else {
      return parent.getItem();
    }
  }

  protected TreeNode<E> findMaximum(TreeNode<E> node) {
    TreeNode<E> current = node;
    while (current.getRight() != null) {
      current = current.getRight();
    }
    return current;
  }

  /**
   * check if tree is a valid red black tree
   *
   * @return true if tree is a valid RB else false
   */
  public boolean isValidTree() {
    return isValidTree(root);
  }

  protected boolean isValidTree(TreeNode<E> node) {
    return isBinarySearchTree(node, null, null) && isBalancedBT(node);
  }

  protected boolean isBinarySearchTree(TreeNode<E> node, E min, E max) {
    if (null == node) {
      return true;
    }
    if (min != null && node.compareTo(min) < 0) {
      return false;
    }
    if (max != null && node.compareTo(max) > 0) {
      return false;
    }
    return isBinarySearchTree(node.getLeft(), min, node.getItem())
      && isBinarySearchTree(node.getRight(), node.getItem(), max);
  }

  protected boolean isBalancedBT(TreeNode<E> node) {
    int blackNodes = 0;
    TreeNode<E> tmp = root;
    while (tmp != null) {
      if (tmp.getColor() == BLACK) {
        blackNodes++;
      }
      tmp = tmp.getLeft();
    }
    return isBalancedBT(root, blackNodes);
  }

  protected boolean isBalancedBT(TreeNode<E> node, int blackNodes) {
    if (null == node) {
      return (0 == blackNodes);
    }
    if (node.getColor() == BLACK) {
      blackNodes--;
    }
    return isBalancedBT(node.getLeft(), blackNodes) && isBalancedBT(node.getRight(), blackNodes);
  }

  @Override
  public boolean autoReclaim() {
    return autoReclaim;
  }

  /**
   * sync. this object
   */
  @Override
  public void syncToVolatileMemory() {

  }

  /**
   * Make any cached changes to this object persistent.
   */
  @Override
  public void syncToNonVolatileMemory() {

  }

  /**
   * flush processors cache for this object
   */
  @Override
  public void syncToLocal() {

  }

  @Override
  public long[][] getNativeFieldInfo() {
    return fieldInfo;
  }

  @Override
  public void refbreak() {
    return;
  }

  @Override
  public void destroy() throws RetrieveDurableEntityError {
    destroy(root);
    holder.destroy();
  }

  protected void destroy(TreeNode<E> node) {
    if (null == node) {
      return;
    }
    destroy(node.getLeft());
    destroy(node.getRight());
    node.setLeft(null, false);
    node.setRight(null, false);
    node.setParent(null, false);
    node.destroy();
  }

  @Override
  public void cancelAutoReclaim() {
    autoReclaim = false;
  }

  @Override
  public void registerAutoReclaim() {
    this.registerAutoReclaim(reclaimcontext);
  }

  @Override
  public void registerAutoReclaim(ReclaimContext rctx) {
    autoReclaim = true;
    reclaimcontext = rctx;
  }

  @Override
  public long getHandler() {
    return allocator.getChunkHandler(holder);
  }

  @Override
  public void restoreDurableEntity(A allocator, EntityFactoryProxy[] factoryProxy,
      DurableType[] gType, long phandler, boolean autoReclaim, ReclaimContext rctx)
          throws RestoreDurableEntityError {
    initializeDurableEntity(allocator, factoryProxy, gType, autoReclaim, rctx);
    if (0L == phandler) {
      throw new RestoreDurableEntityError("Input handler is null on restoreDurableEntity.");
    }
    holder = allocator.retrieveChunk(phandler, autoReclaim);
    long rootHandler = unsafe.getLong(holder.get());
    root = TreeNodeFactory.restore(allocator, factoryProxy, genericType, rootHandler, autoReclaim, reclaimcontext);
    initializeAfterRestore();
  }


  @Override
  public void initializeDurableEntity(A allocator, EntityFactoryProxy[] factoryProxy,
      DurableType[] gType, boolean autoReclaim, ReclaimContext rctx) {
    this.allocator = allocator;
    this.factoryProxy = factoryProxy;
    this.genericType = gType;
    this.autoReclaim = autoReclaim;
    try {
      this.unsafe = Utils.getUnsafe();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void createDurableEntity(A allocator, EntityFactoryProxy[] factoryProxy,
      DurableType[] gType, boolean autoReclaim, ReclaimContext rctx) throws OutOfHybridMemory {
    initializeDurableEntity(allocator, factoryProxy, gType, autoReclaim, rctx);
    this.holder = allocator.createChunk(MAX_OBJECT_SIZE, autoReclaim, reclaimcontext);
    initializeAfterCreate();
  }

}
