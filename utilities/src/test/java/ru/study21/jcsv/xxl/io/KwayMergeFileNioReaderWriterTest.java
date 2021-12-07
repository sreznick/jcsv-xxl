package ru.study21.jcsv.xxl.io;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class KwayMergeFileNioReaderWriterTest {

    @Test
    public void testBasic() throws IOException {
        FileManager manager = FileManager.createTempDirectory("kwaymergefilenioreaderwritertest");
        Path path = manager.createTempFile("one");
        byte[] data = new byte[]{3, 3, 2, 2, 1, 1, 4, 4, 4};
        Files.write(path, data);

        KwayMergeFileNioReaderWriter.setCacheSize(4);

        SeekableByteChannel binChannel = Files.newByteChannel(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
        KwayMergeFileNioReaderWriter thing = new KwayMergeFileNioReaderWriter(binChannel, List.of(
                new KwayMergeFileNioReaderWriter.Region(0, 2),
                new KwayMergeFileNioReaderWriter.Region(2, 2),
                new KwayMergeFileNioReaderWriter.Region(4, 2),
                new KwayMergeFileNioReaderWriter.Region(6, 3)
        ));

        byte[] arr = new byte[2];
        assertEquals(2, thing.read(arr, 0));
        assertArrayEquals(new byte[]{3, 3}, arr);

        assertEquals(2, thing.read(arr, 2));
        assertArrayEquals(new byte[]{1, 1}, arr);

        assertEquals(2, thing.read(arr, 1));
        assertArrayEquals(new byte[]{2, 2}, arr);

        byte[] br = new byte[1];
        assertEquals(1, thing.read(br, 3));
        assertArrayEquals(new byte[]{4}, br);
        assertEquals(2, thing.read(arr, 3));
        assertArrayEquals(new byte[]{4, 4}, arr);

        assertEquals(0, thing.read(arr, 0));
        assertEquals(0, thing.read(br, 3));

        byte[] c = new byte[]{7, 7, 7, 7, 7, 7, 7, 7, 7};
        thing.write(c);

        thing.close();
        binChannel.close();

        assertArrayEquals(c, Files.readAllBytes(path));
    }

}
