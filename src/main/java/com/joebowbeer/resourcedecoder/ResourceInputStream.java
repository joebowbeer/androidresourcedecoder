package com.joebowbeer.resourcedecoder;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ResourceInputStream extends FilterInputStream {

    protected long limit;
    protected long nread;

    public ResourceInputStream(InputStream in) {
        this(in, Long.MAX_VALUE);
    }

    public ResourceInputStream(InputStream in, long byteLimit) {
        super(in);
        this.limit = byteLimit;
    }

    /** Returns absolute offset in resource stream. */
    public long getResourceOffset() {
        return (in instanceof ResourceInputStream)
                ? ((ResourceInputStream) in).getResourceOffset() : nread;
    }

    /** Throws EOFException if EOF reached before all bytes are read. */
    public void readFully(byte[] b) throws IOException {
        for (int off = 0, len = b.length; len > 0; len -= off) {
            int count = read(b, off, len);
            if (count <= 0) {
                throw new EOFException();
            }
            off += count;
        }
    }

    /** Throws EOFException if EOF reached before all bytes are skipped. */
    public void skipFully(int n) throws IOException {
        assert n >= 0;
        if (n != 0) {
            Log.d("skipping " + n + " bytes");
        }
        for (long skipped, remaining = n; remaining > 0; remaining -= skipped) {
            if ((skipped = skip(remaining)) <= 0) {
                throw new EOFException();
            }
        }
    }

    /** See DataInput.readUnsignedByte */
    public int readUnsignedByte() throws IOException {
        int ch = read();
        if (ch < 0) {
            throw new EOFException();
        }
        return ch;
    }

    /** LE version of DataInput.readUnsignedShort */
    public int readUnsignedShort() throws IOException {
        int ch1 = read();
        int ch2 = read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (ch2 << 8) + ch1;
    }

    /** LE version of DataInput.readInt */
    public int readInt() throws IOException {
        int ch1 = read();
        int ch2 = read();
        int ch3 = read();
        int ch4 = read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        return (ch4 << 24) + (ch3 << 16) + (ch2 << 8) + ch1;
    }

    /* InputStream overrides */

    @Override
    public int read() throws IOException {
        if (nread >= limit) {
            return -1;
        }
        int ch = super.read();
        if (ch != -1) {
            nread++;
        }
        return ch;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        if (len + nread > limit) {
            len = (int)(limit - nread);
        }
        if (len <= 0) {
            return -1;
        }
        int count = super.read(b, off, len);
        if (count != -1) {
            nread += count;
        }
        return count;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n + nread > limit) {
            n = limit - nread;
        }
        long count = super.skip(n);
        if (count > 0) {
            nread += count;
        }
        return count;
    }

    @Override
    public int available() throws IOException {
        int count = super.available();
        if (count + nread > limit) {
            count = (int)(limit - nread);
        }
        return count;
    }

    @Override
    public boolean markSupported() {
        return false;
    }
}