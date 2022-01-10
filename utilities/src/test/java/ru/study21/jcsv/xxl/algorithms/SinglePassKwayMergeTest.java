package ru.study21.jcsv.xxl.algorithms;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.io.*;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class SinglePassKwayMergeTest {

    @Test
    public void test() throws IOException, BrokenContentsException {

        // file is as if sorted by batches of size 3
        String presortedContents = """
                4,aa
                5,bb
                7,aa
                2,cc
                3,bb
                5,aa
                8,bb
                8,cc
                """;
        // regions order: 1, 1, 0, 1, 0, 0, 2, 2

        // write source
        FileManager fm = FileManager.createTempDirectory("SinglePassKwayMergeTest");
        Path sourcePath = fm.createTempFile("source");
        Files.writeString(sourcePath, presortedContents);

        Path inputBinPath = fm.createTempFile("binary");
        Path outputBinPath = fm.createTempFile("output");

        // binarize source
        CSVReader sourceReader = new DefaultCSVReader.Builder(Files.newBufferedReader(sourcePath))
                .withoutHeader()
                .build();
        CSVFileBinarizer.IntCTBS intCTBS = new CSVFileBinarizer.IntCTBS();
        CSVFileBinarizer.StringCTBS stringCTBS = new CSVFileBinarizer.StringCTBS(2, StandardCharsets.US_ASCII);
        List<CSVFileBinarizer.ColumnTypeBinarizationParams> binParams = List.of(intCTBS, stringCTBS);

        CSVFileBinarizer.binarize(sourceReader, binParams, inputBinPath);

        // init merger
        SortDescription sortDescription = new SortDescription(List.of(
                SortDescription.asLong(0), SortDescription.asString(1)
        ));
        ExternalKwayMerge merger = new ExternalKwayMerge(binParams, sortDescription);

        try (SeekableByteChannel inputBinChannel = Files.newByteChannel(inputBinPath);
             SeekableByteChannel outputBinChannel = Files.newByteChannel(outputBinPath, StandardOpenOption.WRITE)) {
            MultiregionCachedNioBinaryReader binaryReader = new MultiregionCachedNioBinaryReader(inputBinChannel, List.of(
                    new MultiregionCachedNioBinaryReader.Region(0, 6 * 3),
                    new MultiregionCachedNioBinaryReader.Region(6 * 3, 6 * 3),
                    new MultiregionCachedNioBinaryReader.Region(6 * 6, 6 * 2)
            ));
            CachedNioBinaryWriter binaryWriter = new CachedNioBinaryWriter(outputBinChannel);

            merger.singlePassKwayMerge(
                    binaryReader,
                    binaryWriter,
                    3,
                    6
            );

            binaryWriter.close();

            byte[] b = Files.readAllBytes(outputBinPath);

            Assertions.assertArrayEquals(new byte[]{
                    0, 0, 0, 2, 'c', 'c',
                    0, 0, 0, 3, 'b', 'b',
                    0, 0, 0, 4, 'a', 'a',
                    0, 0, 0, 5, 'a', 'a',
                    0, 0, 0, 5, 'b', 'b',
                    0, 0, 0, 7, 'a', 'a',
                    0, 0, 0, 8, 'b', 'b',
                    0, 0, 0, 8, 'c', 'c'
            }, Files.readAllBytes(outputBinPath));
        }
    }

}
