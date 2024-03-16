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

package org.apache.mnemonic.bench;

/**
 * Represents a node in a linked list.
 *
 * @param <T> the type of data stored in the node
 */
public class Node<T> {

    private T data; // The data stored in the node
    private Node<T> nextNode; // Reference to the next node in the list

    /**
     * Constructs a new node with the given data.
     *
     * @param data the data to be stored in the node
     */
    public Node(T data) {
        this.data = data;
        this.nextNode = null;
    }

    /**
     * Constructs a new node with the given data and reference to the next node.
     *
     * @param data     the data to be stored in the node
     * @param nextNode the next node in the list
     */
    public Node(T data, Node<T> nextNode) {
        this.data = data;
        this.nextNode = nextNode;
    }

    /**
     * Gets the data stored in the node.
     *
     * @return the data stored in the node
     */
    public T getData() {
        return this.data;
    }

    /**
     * Sets the reference to the next node.
     *
     * @param nextNode the next node in the list
     */
    public void setNext(Node<T> nextNode) {
        this.nextNode = nextNode;
    }

    /**
     * Gets the next node in the list.
     *
     * @return the next node in the list
     */
    public Node<T> getNext() {
        return this.nextNode;
    }
}

