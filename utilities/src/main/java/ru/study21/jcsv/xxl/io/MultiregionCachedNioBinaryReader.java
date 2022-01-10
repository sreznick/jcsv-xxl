package ru.study21.jcsv.xxl.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MultiregionCachedNioBinaryReader implements AutoCloseable {

    private SeekableByteChannel binChannel;

    // read from given positions given lengths
    // write to autoincr position sequentially

    private final List<ByteBuffer> readCaches = new ArrayList<>();
    private final List<Region> regions;
    private final List<Long> regionsPos;
    private final boolean[] didInit;

    private static int CACHE_SIZE = 8196; // 65536 can do better for large files (?)

    // use with caution!
    public static void setCacheSize(int cacheSize) {
        CACHE_SIZE = cacheSize;
    }

    public static record Region(long start, long len) {
        public boolean isConsecutive(Region other) {
            return start + len == other.start;
        }
    }

    // Channel is required to be both readable and writeable
    public MultiregionCachedNioBinaryReader(SeekableByteChannel binChannel, List<Region> regions) {
        for (int i = 1; i < regions.size(); i++) {
            if (!regions.get(i - 1).isConsecutive(regions.get(i))) {
                throw new IllegalArgumentException("only consecutive regions are supported");
            }
        }
        IntStream.range(0, regions.size()).forEach(i ->
                readCaches.add(ByteBuffer.allocate(CACHE_SIZE)));
        this.regions = regions;

        regionsPos = regions.stream().map(Region::start).collect(Collectors.toCollection(ArrayList::new));
        this.binChannel = binChannel;
        didInit = new boolean[regions.size()];
    }

    private void readRegion(int region) throws IOException {
        ByteBuffer curCache = readCaches.get(region);
        curCache.clear();
        binChannel.position(regionsPos.get(region));
        binChannel.read(curCache);
        curCache.flip();
    }

    public int read(byte[] arr, int region) throws IOException {
        if (binChannel == null) {
            throw new IllegalStateException("cannot read after close");
        }
        Region curRegion = regions.get(region);
        int logicLen = Math.toIntExact(Math.min(
                arr.length,
                curRegion.start + curRegion.len - regionsPos.get(region)
        ));
        assert logicLen >= 0;
        assert logicLen <= curRegion.len;
        assert regionsPos.get(region) + logicLen <= curRegion.start + curRegion.len;

        if (!didInit[region]) {
            readRegion(region);
            didInit[region] = true;
        }

        ByteBuffer curCache = readCaches.get(region);
        int curRem = curCache.remaining();
        if (curRem >= logicLen) {
            curCache.get(arr, 0, logicLen);
        } else {
            curCache.get(arr, 0, curRem);
            regionsPos.set(region, regionsPos.get(region) + curRem);

            readRegion(region);

            if (curCache.remaining() < logicLen - curRem) {
                throw new IllegalStateException("file shorter than expected OR extra read required");
                // TODO (?): support needing to read multiple times
                // (although the whole point of this class is to use small arrays)
            }
            curCache.get(arr, curRem, logicLen - curRem);
        }
        regionsPos.set(region, regionsPos.get(region) + logicLen);
        return logicLen;
    }

    @Override
    public void close() {
        binChannel = null;
    }

}