package ru.study21.jcsv.xxl.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class CachedNioBinaryWriterTest {

    @Test
    public void test() throws IOException {
        Path path = FileManager.createTempDirectory("cachedniobinarywritertest").createTempFile("file");

        CachedNioBinaryWriter.setCacheSize(4);
        CachedNioBinaryWriter writer = new CachedNioBinaryWriter(Files.newByteChannel(path, StandardOpenOption.WRITE));

        byte[] arr = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
        writer.write(arr);
        writer.close();

        Assertions.assertArrayEquals(arr, Files.readAllBytes(path));
    }

}
