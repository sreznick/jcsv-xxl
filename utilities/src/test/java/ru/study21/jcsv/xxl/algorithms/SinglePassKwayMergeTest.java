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

            merger.doKwayMerge(
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

    @Test
    public void testMultipleRegions() throws IOException {
        FileManager fm = FileManager.createTempDirectory("SinglePassKwayMergeTest");

        Path inputBinPath = fm.createTempFile("binary");
        Path outputBinPath = fm.createTempFile("output");

        byte[] input = new byte[]{
                'g', 'h',
                'e', 'f',
                'c', 'd',
                'a', 'b'
        };
        Files.write(inputBinPath, input, StandardOpenOption.WRITE);

        CSVFileBinarizer.StringCTBS stringCTBS = new CSVFileBinarizer.StringCTBS(1, StandardCharsets.US_ASCII);
        SortDescription sd = SortDescription.of(SortDescription.asString(0));
        ExternalKwayMerge merger = new ExternalKwayMerge(List.of(stringCTBS, stringCTBS), sd);

        // clear output file
        Files.write(outputBinPath, new byte[]{}, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

        try (
                SeekableByteChannel inputBinChannel = Files.newByteChannel(inputBinPath,
                        StandardOpenOption.READ);
                SeekableByteChannel outputBinChannel = Files.newByteChannel(outputBinPath,
                        StandardOpenOption.WRITE)
        ) {
            // first half
            MultiregionCachedNioBinaryReader binaryReader1 = new MultiregionCachedNioBinaryReader(inputBinChannel, List.of(
                    new MultiregionCachedNioBinaryReader.Region(0, 2),
                    new MultiregionCachedNioBinaryReader.Region(2, 2)
            ));
            CachedNioBinaryWriter binaryWriter1 = new CachedNioBinaryWriter(outputBinChannel);

            merger.doKwayMerge(
                    binaryReader1,
                    binaryWriter1,
                    2,
                    1
            );
            binaryWriter1.close();
        }

        try (
                SeekableByteChannel inputBinChannel = Files.newByteChannel(inputBinPath,
                        StandardOpenOption.READ);
                SeekableByteChannel outputBinChannel = Files.newByteChannel(outputBinPath,
                        StandardOpenOption.WRITE, StandardOpenOption.APPEND)
        ) {
            //second half
            MultiregionCachedNioBinaryReader binaryReader2 = new MultiregionCachedNioBinaryReader(inputBinChannel, List.of(
                    new MultiregionCachedNioBinaryReader.Region(4, 2),
                    new MultiregionCachedNioBinaryReader.Region(6, 2)
            ));
            CachedNioBinaryWriter binaryWriter2 = new CachedNioBinaryWriter(outputBinChannel);

            merger.doKwayMerge(
                    binaryReader2,
                    binaryWriter2,
                    2,
                    1
            );
            binaryWriter2.close();
        }

        // check before third merge
        Assertions.assertArrayEquals(new byte[]{
                'e', 'f', 'g', 'h',
                'a', 'b', 'c', 'd'
        }, Files.readAllBytes(outputBinPath));

        // halves together
        // invert input / output !
        try (
                SeekableByteChannel newInputBinChannel = Files.newByteChannel(outputBinPath,
                        StandardOpenOption.READ);
                SeekableByteChannel newOutputBinChannel = Files.newByteChannel(inputBinPath,
                        StandardOpenOption.WRITE)
        ) {
            MultiregionCachedNioBinaryReader binaryReader3 = new MultiregionCachedNioBinaryReader(newInputBinChannel, List.of(
                    new MultiregionCachedNioBinaryReader.Region(0, 4),
                    new MultiregionCachedNioBinaryReader.Region(4, 4)
            ));
            CachedNioBinaryWriter binaryWriter3 = new CachedNioBinaryWriter(newOutputBinChannel);

            merger.doKwayMerge(
                    binaryReader3,
                    binaryWriter3,
                    2,
                    1
            );
            binaryWriter3.close();
        }

        Assertions.assertArrayEquals(new byte[]{
                'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'
        }, Files.readAllBytes(inputBinPath));

    }

}
