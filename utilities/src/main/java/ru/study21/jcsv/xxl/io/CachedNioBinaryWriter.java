package ru.study21.jcsv.xxl.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SeekableByteChannel;

public class CachedNioBinaryWriter implements AutoCloseable {

    private final int CACHE_SIZE; // 65536 for large files?
    private final ByteBuffer writeCache;
    private final ByteChannel binChannel;

    public CachedNioBinaryWriter(ByteChannel binChannel, int cacheSize) {
        CACHE_SIZE = cacheSize;
        this.binChannel = binChannel;
        writeCache = ByteBuffer.allocate(CACHE_SIZE);
    }

    private void flushWriteCache() throws IOException {
        writeCache.flip();
        binChannel.write(writeCache);
        writeCache.clear();
    }

    public void write(byte[] arr) throws IOException {
        int done = 0;
        while (done < arr.length) {
            int len = Math.min(arr.length - done, writeCache.remaining());
            if (len == 0) { // does not do an extra flush when done == arr.length
                flushWriteCache();
                continue;
            }
            writeCache.put(arr, done, len);
            done += len;
        }
    }

    @Override
    public void close() throws IOException {
        flushWriteCache();
//        binChannel.close(); this is VERY rude
    }
}
