package com.intel.bigdatamem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * an input Stream that is backed by a in-memory ByteBuffer.
 * 
 *
 */
public class ByteBufferBackedInputStream extends InputStream {

    private ByteBuffer buf;

    /**
     * accept a ByteBuffer as backed object for inputStream.
     * 
     * @param buf
     *            specify a bytebuffer that is where any data is from
     */
    public ByteBufferBackedInputStream(ByteBuffer buf) {
	this.buf = buf;
    }

    /**
     * read an integer value from backed ByteBuffer.
     * 
     * @return a integer value from stream input
     */
    public int read() throws IOException {
	if (!buf.hasRemaining()) {
	    return -1;
	}
	return buf.get() & 0xFF;
    }

    /**
     * read a specified range of byte array from backed ByteBuffer.
     * 
     * @param bytes
     *            specify a output byte array to store data
     * 
     * @param off
     *            specify the offset from ByteBuffer to read
     * 
     * @param len
     *            specify the length of bytes to read
     * 
     * @return the number of bytes has been read
     */
    public int read(byte[] bytes, int off, int len) throws IOException {
	if (!buf.hasRemaining()) {
	    return -1;
	}

	len = Math.min(len, buf.remaining());
	buf.get(bytes, off, len);
	return len;
    }
}
