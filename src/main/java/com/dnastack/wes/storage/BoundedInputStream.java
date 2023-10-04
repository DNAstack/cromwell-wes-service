package com.dnastack.wes.storage;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class BoundedInputStream extends InputStream {


    /**
     * The wrapped input stream.
     */
    private final InputStream inputStream;


    /**
     * The limit, -1 if none.
     */
    private final long maxPos;


    /**
     * The current position of the inner input stream.
     */
    private long pos;

    /**
     * The offset at which to start reading bytes in the inner input stream.
     */
    private final long minPos;


    /**
     * Marks the input stream.
     */
    private long mark;


    /**
     * Creates a new bounded input stream.
     *
     * @param in   The input stream to wrap.
     * @param size The maximum number of bytes to return, -1 if no limit.
     */
    public BoundedInputStream(final InputStream in, final long size) {
        this(in, 0L, size);
    }

    /**
     * Creates a new bounded input stream.
     *
     * @param in   The input stream to wrap.
     * @param minPos  The starting offset for the bounded input stream; bytes before the offset will be skipped.
     * @param size The maximum number of bytes to return, -1 if no limit.
     */
    public BoundedInputStream(final InputStream in, final long minPos, final long size) {
        this.minPos = minPos;
        this.pos = 0L;
        this.mark = -1L;
        this.maxPos = size >= 0 ? minPos + size : -1L;
        this.inputStream = in;
    }

    @Override
    public int read() throws IOException {
        if (isEndOfStream()) {
            return -1;
        }
        skipBytesBeforeOffset();
        int result = this.inputStream.read();
        if (result >= 0) {
            this.pos++;
        }
        return result;
    }

    @Override
    public int read(@NotNull byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        if (isEndOfStream()) {
            return -1;
        }
        skipBytesBeforeOffset();
        if (shouldLimitLength(len)) {
            len = (int) (this.maxPos - this.pos);
        }
        int bytesRead = this.inputStream.read(b, off, len);
        if (bytesRead == -1) {
            return -1;
        } else {
            this.pos += bytesRead;
            return bytesRead;
        }
    }

    private boolean isEndOfStream() {
        return this.maxPos >= 0L && this.pos >= this.maxPos;
    }

    private boolean shouldLimitLength(int len) {
        return (this.pos + len > this.maxPos) && this.maxPos != -1;
    }

    @Override
    public long skip(long n) throws IOException {
        long toSkip = this.maxPos >= 0L ? Math.min(n, this.maxPos - this.pos) : n;
        long skippedBytes = this.inputStream.skip(toSkip);
        this.pos += skippedBytes;
        return skippedBytes;
    }


    @Override
    public int available() throws IOException {
        return this.maxPos >= 0L && this.pos >= this.maxPos ? 0 : this.inputStream.available();
    }


    @Override
    public String toString() {
        return this.inputStream.toString();
    }


    @Override
    public void close() throws IOException {
        this.inputStream.close();
    }


    @Override
    public synchronized void reset() throws IOException {
        this.inputStream.reset();
        this.pos = this.mark;
    }


    @Override
    public synchronized void mark(int readlimit) {
        this.inputStream.mark(readlimit);
        this.mark = this.pos;
    }


    @Override
    public boolean markSupported() {
        return this.inputStream.markSupported();
    }

    /**
     * Try skipping bytes to the offset with the internal stream.
     */
    private void skipBytesBeforeOffset() throws IOException {
        final long toSkip = this.minPos - this.pos;
        if (toSkip > 0) {
            final long actuallySkipped = this.inputStream.skip(toSkip);
            this.pos += actuallySkipped;
            if (actuallySkipped != toSkip) {
                log.debug("Could not skip {} bytes. Instead skipped {} bytes.", toSkip, actuallySkipped);
            }
        }
    }

}