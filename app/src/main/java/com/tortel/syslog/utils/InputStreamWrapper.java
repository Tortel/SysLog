package com.tortel.syslog.utils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Input stream that replaces \n with \r\n
 */
public class InputStreamWrapper extends InputStream {
    private InputStream is;

    public InputStreamWrapper(InputStream in) {
        is = in;
    }

    @Override
    public int read() throws IOException {
        return is.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = 0, c;
        do {
            c = this.read();
            if(c != -1) {
                if(c == '\n') {
                    if(len >= 2){
                        b[off + n] = '\r';
                        n++;
                        len--;
                    }
                }
                b[off + n] = (byte) c;
                n++;
                len--;
            }
        } while(c != -1 && len > 0);
        return n;
    }

    @Override
    public int available() throws IOException {
        return is.read();
    }

    @Override
    public boolean equals(Object o) {
        return is.equals(o);
    }

    @Override
    public void close() throws IOException {
        is.close();
    }

    @Override
    public void mark(int readlimit) {
        is.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return is.markSupported();
    }

    @Override
    public int hashCode() {
        return is.hashCode();
    }

    @Override
    public String toString() {
        return is.toString();
    }

    @Override
    public synchronized void reset() throws IOException {
        is.reset();
    }

    @Override
    public long skip(long byteCount) throws IOException {
        return is.skip(byteCount);
    }
}
