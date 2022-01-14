package ru.study21.jcsv.xxl.io;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class MultiregionCachedNioBinaryReaderTest {

    @Test
    public void testBasic() throws IOException {
        FileManager manager = FileManager.createTempDirectory("kwaymergefilenioreaderwritertest");
        Path path = manager.createTempFile("one");
        byte[] data = new byte[]{3, 3, 2, 2, 1, 1, 4, 4, 4};
        Files.write(path, data);

        SeekableByteChannel binChannel = Files.newByteChannel(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
        MultiregionCachedNioBinaryReader thing = new MultiregionCachedNioBinaryReader(binChannel, List.of(
                new MultiregionCachedNioBinaryReader.Region(0, 2),
                new MultiregionCachedNioBinaryReader.Region(2, 2),
                new MultiregionCachedNioBinaryReader.Region(4, 2),
                new MultiregionCachedNioBinaryReader.Region(6, 3)
        ), 4);

        byte[] arr = new byte[2];
        thing.read(arr, 0);
        assertArrayEquals(new byte[]{3, 3}, arr);

        thing.read(arr, 2);
        assertArrayEquals(new byte[]{1, 1}, arr);

        thing.read(arr, 1);
        assertArrayEquals(new byte[]{2, 2}, arr);

        byte[] br = new byte[1];
        thing.read(br, 3);
        assertArrayEquals(new byte[]{4}, br);
        thing.read(arr, 3);
        assertArrayEquals(new byte[]{4, 4}, arr);

//        assertEquals(0, thing.read(arr, 0));
//        assertEquals(0, thing.read(br, 3));
    }

}
