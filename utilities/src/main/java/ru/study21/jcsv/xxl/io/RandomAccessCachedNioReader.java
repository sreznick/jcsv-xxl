package ru.study21.jcsv.xxl.io;

import ru.study21.jcsv.xxl.common.ManualByteBufferAllocator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Random;

// assumes underlying file is not updated
public class RandomAccessCachedNioReader {

    private final ByteBuffer[] caches;
    private final long[] positions;
    private final FileChannel channel;
    private final CachePolicy cachePolicy;

    private long cacheMissCnt;
    private long readCnt;

    public interface CachePolicy {
        int readCache(long pos) throws IOException; // return index of new cache

        default void didRead(int cacheIndex) { /* do nothing */ } // trigger to calc stats (for LRU / ...)

        enum Type {RR} // TODO: LRU requires heap with decrease-key, use already implemented PairingHeap
    }

    private class RRPolicy implements CachePolicy {
        private final Random random = new Random(5);

        @Override
        public int readCache(long pos) throws IOException {
            int index = random.nextInt(caches.length);
            caches[index].clear();

//            channel.read(caches[index], pos); // might not fill to capacity!
            channel.position(pos);
            channel.read(caches[index]);

            positions[index] = pos;
            return index;
        }
    }

    public RandomAccessCachedNioReader(
            FileChannel channel,
            long approxMemoryLimit,
            int cacheCount, // defines cache size,
            CachePolicy.Type cacheType
    ) {
        this.channel = channel;

        if (approxMemoryLimit / cacheCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("too large cache size");
        }
        int cacheSize = (int) (approxMemoryLimit / cacheCount);
//        caches = new ByteBuffer[cacheCount];
        caches = ManualByteBufferAllocator.allocate(cacheCount, cacheSize);
        positions = new long[cacheCount];
        for (int i = 0; i < cacheCount; i++) {
//            caches[i] = ByteBuffer.allocate(cacheSize); // OOM
            positions[i] = Long.MAX_VALUE; // ensure no cache works until written at least once
        }

        cachePolicy = switch (cacheType) {
            case RR -> new RRPolicy();
            // case LRU -> new LRUPolicy();
            default -> throw new IllegalArgumentException("unknown cache type");
        };
    }

    public void read(byte[] arr, long pos) throws IOException {
        readCnt++;
        if(readCnt % 10000 == 0) {
            System.out.println(readCnt + " / " + missRate());
        }

        int cacheIndex = fittingCache(pos, arr.length);
        if (cacheIndex == -1) {
            cacheMissCnt++;
            cacheIndex = cachePolicy.readCache(pos);
        }
        cachePolicy.didRead(cacheIndex);
        ByteBuffer cache = caches[cacheIndex];

        cache.position((int) (pos - positions[cacheIndex]));
        if (cache.remaining() < arr.length) {
            throw new IllegalStateException("cache too small for read of length " + arr.length);
        }
        cache.get(arr);
    }

    // O(cacheCount), a skip-list might be better for storing positions, but does this matter?
    private int fittingCache(long pos, int len) {
        for (int i = 0; i < caches.length; i++) {
            if (positions[i] <= pos && positions[i] + caches[i].capacity() >= pos + len) {
                return i;
            }
        }
        return -1;
    }

    public double missRate() {
        return (double) cacheMissCnt / readCnt;
    }

}
