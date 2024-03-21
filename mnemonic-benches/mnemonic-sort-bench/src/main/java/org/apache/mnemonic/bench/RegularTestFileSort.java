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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 * RegularTestFileSort implements the TextFileSort interface for sorting long values in a text file.
 */
public class RegularTestFileSort implements TextFileSort {

    private Node<Long> head; // Head node of the linked list
    private long[] sortinfo = new long[3]; // Array to store sorting information: [scans, swaps, no swaps]

    /**
     * Constructs a RegularTestFileSort object.
     */
    public RegularTestFileSort() {
    }

    /**
     * Loads data from a BufferedReader into the linked list.
     *
     * @param reader the BufferedReader to read data from
     * @throws NumberFormatException if the data read is not a valid long
     * @throws IOException           if an I/O error occurs while reading
     */
    @Override
    public void load(BufferedReader reader) throws NumberFormatException, IOException {
        String text;
        Node<Long> currentNode = null;
        Long value;
        while ((text = reader.readLine()) != null) {
            value = Long.parseLong(text);
            if (currentNode == null) {
                currentNode = new Node<>(value);
                this.head = currentNode;
            } else {
                currentNode.setNext(new Node<>(value));
                currentNode = currentNode.getNext();
            }
        }
    }

    /**
     * Performs bubble sort on the linked list.
     */
    @Override
    public void doSort() {
        Node<Long> currentNode, tmpNode, prevNode;
        long scanCount = 0L, swapCount = 0L, noSwapCount = 0L;
        boolean changed;
        if (this.head == null) {
            return;
        }
        do {
            ++scanCount;
            currentNode = this.head;
            prevNode = null;
            changed = false;
            while (true) {
                tmpNode = currentNode.getNext();
                if (tmpNode == null) {
                    break;
                }
                if (currentNode.getData().compareTo(tmpNode.getData()) > 0) {
                    currentNode.setNext(tmpNode.getNext());
                    tmpNode.setNext(currentNode);
                    if (prevNode == null) {
                        this.head = tmpNode;
                    } else {
                        prevNode.setNext(tmpNode);
                    }
                    prevNode = tmpNode;
                    changed = true;
                    ++swapCount;
                } else {
                    prevNode = currentNode;
                    currentNode = tmpNode;
                    ++noSwapCount;
                }
            }
        } while (changed);
        this.sortinfo[0] = scanCount;
        this.sortinfo[1] = swapCount;
        this.sortinfo[2] = noSwapCount;
    }

    /**
     * Stores sorted data from the linked list into a BufferedWriter.
     *
     * @param writer the BufferedWriter to write data to
     * @throws IOException if an I/O error occurs while writing
     */
    @Override
    public void store(BufferedWriter writer) throws IOException {
        Node<Long> currentNode = this.head;
        while (currentNode != null) {
            writer.write(currentNode.getData().toString());
            writer.newLine();
            currentNode = currentNode.getNext();
        }
    }

    /**
     * Retrieves sorting information.
     *
     * @return an array containing sorting information: [scans, swaps, no swaps]
     */
    @Override
    public long[] getSortInfo() {
        return this.sortinfo;
    }

    /**
     * Clears the linked list.
     */
    @Override
    public void clear() {
        this.head = null;
    }
}

