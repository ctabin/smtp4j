package ch.astorm.smtp4j.util;

import java.io.IOException;
import java.io.InputStream;

public class MaxMessageSizeInputStream extends InputStream {

    private final long maxMessageSize;
    private final InputStream delegate;

    private long readBytes;

    public MaxMessageSizeInputStream(long maxMessageSize, InputStream delegate) {
        this.maxMessageSize = maxMessageSize;
        this.delegate = delegate;
    }

    @Override
    public int read() throws IOException {
        int ret = delegate.read();
        this.readBytes++;
        assertMaxSize();
        return ret;
    }

    private void assertMaxSize() throws IOException {
        if (readBytes >= maxMessageSize) {
            throw new IOException("Message size exceeded: " + readBytes + " >= " + maxMessageSize);
        }
    }

    private int getMaxSizeToRead(int requestedBytes) throws IOException {
        long maxMessageSizeLeft = maxMessageSize - readBytes;
        int ret = Math.min(requestedBytes, maxMessageSizeLeft > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) maxMessageSizeLeft);
        if (ret <= 0) {
            throw new IOException("Message size exceeded: " + readBytes + " >= " + maxMessageSize);
        }
        return ret;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int readBytes = delegate.read(b, 0, getMaxSizeToRead(b.length));

        readBytes += readBytes;
        assertMaxSize();

        return readBytes;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int readBytes = delegate.read(b, off, getMaxSizeToRead(len));

        this.readBytes += readBytes;
        assertMaxSize();

        return readBytes;
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        int readBytes = delegate.readNBytes(b, off, getMaxSizeToRead(len));

        this.readBytes += readBytes;
        assertMaxSize();

        return readBytes;
    }

    @Override
    public long skip(long n) throws IOException {
        return delegate.skip(n);
    }

    @Override
    public void skipNBytes(long n) throws IOException {
        delegate.skipNBytes(n);
    }

    @Override
    public int available() throws IOException {
        return delegate.available();
    }

    @Override
    public void mark(int readlimit) {
        delegate.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return delegate.markSupported();
    }

    @Override
    public void reset() throws IOException {
        delegate.reset();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
