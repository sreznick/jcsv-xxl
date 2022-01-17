package ru.study21.jcsv.xxl.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class CSVOffsetExtractorTest {

    @Test
    public void test() throws IOException {
        FileManager fm = FileManager.createTempDirectory("csvoffsetextractortest");
        Path inputFile = fm.createTempFile("input");
        Path outputFile = fm.createTempFile("output");

        Files.writeString(inputFile, """
                1
                11
                111
                """);

        FileChannel inputChannel = FileChannel.open(inputFile, StandardOpenOption.READ);
        FileChannel outputChannel = FileChannel.open(outputFile, StandardOpenOption.WRITE);

        Assertions.assertEquals(0, CSVOffsetExtractor.extractOffsets(
                inputChannel,
                outputChannel,
                8192,
                8192,
                false
        ));

        inputChannel.close();
        outputChannel.close();

        // for LF (!) (seems like writeString does NOT convert LF to CRLF)
        Assertions.assertArrayEquals(new byte[]{
                0, 0, 0, 0, 0, 0, 0, 1,
                0, 0, 0, 0, 0, 0, 0, 4,
                0, 0, 0, 0, 0, 0, 0, 8
        }, Files.readAllBytes(outputFile));
    }

    @Test
    public void testHeader() throws IOException {
        FileManager fm = FileManager.createTempDirectory("csvoffsetextractortestheader");
        Path inputFile = fm.createTempFile("input");
        Path outputFile = fm.createTempFile("output");

        Files.writeString(inputFile, """
                a,b
                1,2
                334,4
                """);

        FileChannel inputChannel = FileChannel.open(inputFile, StandardOpenOption.READ);
        FileChannel outputChannel = FileChannel.open(outputFile, StandardOpenOption.WRITE);

        Assertions.assertEquals(3, CSVOffsetExtractor.extractOffsets(
                inputChannel,
                outputChannel,
                16,
                16,
                true
        ));

        inputChannel.close();
        outputChannel.close();

        // for LF (!) (seems like writeString does NOT convert LF to CRLF)
        Assertions.assertArrayEquals(new byte[]{
                0, 0, 0, 0, 0, 0, 0, 7,
                0, 0, 0, 0, 0, 0, 0, 13
        }, Files.readAllBytes(outputFile));
    }

}
