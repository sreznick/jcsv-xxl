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

        CSVOffsetExtractor.extractOffsets(
                inputChannel,
                outputChannel,
                8192,
                8192
        );

        inputChannel.close();
        outputChannel.close();

        // for LF (!) (seems like writeString does NOT convert LF to CRLF)
        Assertions.assertArrayEquals(new byte[]{
                0, 0, 0, 0, 0, 0, 0, 1,
                0, 0, 0, 0, 0, 0, 0, 4,
                0, 0, 0, 0, 0, 0, 0, 8
        }, Files.readAllBytes(outputFile));
    }

}
