package ru.study21.jcsv.xxl.io;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.*;

public class RandomAccessCachedNioReaderTest {

    @Test
    public void basicTest() throws IOException {
        Path file = FileManager.createTempDirectory("RandomAccessCachedNioReaderTest").createTempFile("basicTest");
        Files.write(file, new byte[]{0, 1, 2, 3});

        FileChannel channel = FileChannel.open(file, StandardOpenOption.READ);
        RandomAccessCachedNioReader reader = new RandomAccessCachedNioReader(
                channel,
                4,
                2,
                RandomAccessCachedNioReader.CachePolicy.Type.RR
        );
        byte[] arr = new byte[2];
        reader.read(arr, 0);
        assertArrayEquals(new byte[]{0, 1}, arr);
        reader.read(arr, 2);
        assertArrayEquals(new byte[]{2, 3}, arr);
        reader.read(arr, 1);
        assertArrayEquals(new byte[]{1, 2}, arr);

        assertThrows(IllegalStateException.class, () -> reader.read(new byte[3], 0));
    }

}
