package com.intel.bigdatamem;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * an output Stream that is backed by a in-memory ByteBuffer.
 * 
 *
 */
public class ByteBufferBackedOutputStream extends OutputStream {

    private ByteBuffer buf;

    /**
     * accept a ByteBuffer to store external data, the capacity of it could be
     * extended at will.
     * 
     * @param buf
     *            specify a ByteBuffer object that is used to store external
     *            data to its backed buffer
     * 
     */
    public ByteBufferBackedOutputStream(ByteBuffer buf) {
	this.buf = buf;
    }

    /**
     * write an integer value to backed buffer.
     * 
     * @param b
     *            specify an integer value to be written
     */
    public void write(int b) throws IOException {
	buf.put((byte) b);
    }

    /**
     * write an array of bytes to a specified range of backed buffer
     * 
     * @param bytes
     *            specify a byte array to write
     * 
     * @param off
     *            specify the offset of backed buffer where is start point to be
     *            written
     * 
     * @param len
     *            specify the length of bytes to be written
     */
    public void write(byte[] bytes, int off, int len) throws IOException {
	buf.put(bytes, off, len);
    }

}
