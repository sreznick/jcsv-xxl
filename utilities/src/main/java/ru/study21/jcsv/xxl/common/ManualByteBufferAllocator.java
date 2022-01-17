package ru.study21.jcsv.xxl.common;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// prevents OOM
public class ManualByteBufferAllocator {

    private static List<ByteBuffer> bufs = new ArrayList<>();

    // larger allocation size ==> less space wasted, more likely to OOM
    private static int allocationSize = 1 << 29; // 0.5 GB

    // use with caution
    public static void setAllocationSize(int newAllocationSize) {
        allocationSize = newAllocationSize;
    }

    // ATTENTION: the old buffers STILL CAN access data!
    // GC better take care of them...
    public static ByteBuffer[] allocate(int count, int size) {
        System.out.println("Allocating " + count + " x " + size + " = " + ((long) count * size));
        try {
            if (size > allocationSize) {
                throw new IllegalArgumentException("requested buffers too large");
            }
            if (bufs.size() == 0) {
                bufs.add(ByteBuffer.allocate(allocationSize));
            }
            ByteBuffer[] result = new ByteBuffer[count];
            int buf = 0;
            int used = 0;
            for (int i = 0; i < count; i++) {
                if (allocationSize - used < size) {
                    buf++;
                    if (bufs.size() == buf) {
                        bufs.add(ByteBuffer.allocate(allocationSize));
                    }
                    used = 0;
                }
                result[i] = bufs.get(buf).slice(used, size);
                used += size;
            }
            return result;
        } catch (OutOfMemoryError e) {
            System.out.println("=====");
            System.out.println(Runtime.getRuntime().freeMemory());
            System.out.println(Runtime.getRuntime().totalMemory());
            System.out.println(Runtime.getRuntime().maxMemory());
            System.out.println(bufs.size());
            System.out.println((long) bufs.size() * allocationSize);
            System.out.println("+++++");
            throw e;
        }
    }

    // this won't do much unless old buffers are GC'd
    public static void deallocate() {
        bufs = new ArrayList<>();
    }

}
