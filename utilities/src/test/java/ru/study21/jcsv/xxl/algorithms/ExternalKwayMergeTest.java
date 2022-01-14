package ru.study21.jcsv.xxl.algorithms;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.io.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ExternalKwayMergeTest {

    private List<CSVFileBinarizer.ColumnTypeBinarizationParams> binParams;
    private SortDescription sd;
    private Path inputFile, outputFile;

    private final CSVFileBinarizer.IntCTBS intCTBS = new CSVFileBinarizer.IntCTBS();
    private final CSVFileBinarizer.StringCTBS stringCTBS = new CSVFileBinarizer.StringCTBS(1, StandardCharsets.US_ASCII);

    @BeforeEach
    public void setup() throws IOException {
        FileManager fm = FileManager.createTempDirectory("testSinglePassMerge");
        inputFile = fm.createTempFile("input");
        outputFile = fm.createTempFile("output");
        String contents = """
                5,c
                2,a
                1,b
                5,a
                8,b
                7,a
                4,a
                8,a
                """;
        Files.writeString(inputFile, contents);

        binParams = List.of(intCTBS, stringCTBS);
        sd = SortDescription.of(List.of(
                SortDescription.asLong(0),
                SortDescription.asString(1)
        ));
    }

    @Test
    public void testSinglePassMerge() throws IOException, BrokenContentsException {
        try (
                BufferedReader r = Files.newBufferedReader(inputFile);
                BufferedWriter w = Files.newBufferedWriter(outputFile)
        ) {
            CSVReader reader = DefaultCSVReader.builder(r).withoutHeader().build();
            CSVWriter writer = new DefaultCSVWriter(w);

            ExternalKwayMerge merger = new ExternalKwayMerge(
                    binParams, sd
            );
            merger.singlePassMerge(reader, writer, 256 * (1L << 20), 8192);
        }

        String result = Files.readString(outputFile);
        result = result.replace(System.lineSeparator(), "\n"); // CRLF to LF
        Assertions.assertEquals("""
                1,b
                2,a
                4,a
                5,a
                5,c
                7,a
                8,a
                8,b
                """, result);
    }

    // is limited by interface
    @Test
    public void testDoublePassMerge() throws IOException, BrokenContentsException {
        try (
                BufferedReader r = Files.newBufferedReader(inputFile);
                BufferedWriter w = Files.newBufferedWriter(outputFile)
        ) {
            CSVReader reader = DefaultCSVReader.builder(r).withoutHeader().build();
            CSVWriter writer = new DefaultCSVWriter(w);

            ExternalKwayMerge merger = new ExternalKwayMerge(
                    binParams, sd
            );
            merger.doublePassMerge(reader, writer, 256 * (1L << 20), 4, 8192);
        }

        String result = Files.readString(outputFile);
        result = result.replace(System.lineSeparator(), "\n"); // CRLF to LF
        Assertions.assertEquals("""
                1,b
                2,a
                4,a
                5,a
                5,c
                7,a
                8,a
                8,b
                """, result);
    }

    private static record SortItem(int number, char symbol) {
        @Override
        public String toString() {
            return number + "," + symbol;
        }
    }

    private int randomInt(Random random) {
        return random.nextInt(127);
    }

    private char randomChar(Random random) {
        return (char) ('a' + random.nextInt('z' - 'a'));
    }

    @Test
    // runs for 1-2 minutes
    public void stressTest() throws IOException, BrokenContentsException {
        Random random = new Random(5);

        for (int round = 0; round < 1000; round++) {

            // generate data larger than memLimit
            int length = random.nextInt(1 << 12) + 10; // not divisible evenly
            List<SortItem> sample = IntStream.range(0, length).mapToObj(i -> new SortItem(randomInt(random), randomChar(random))).collect(Collectors.toList());
            String content = sample.stream().map(SortItem::toString).collect(Collectors.joining("\n"));
            Files.writeString(inputFile, content, StandardOpenOption.TRUNCATE_EXISTING);

            sample.sort(Comparator.comparing(SortItem::number).thenComparing(SortItem::symbol));
            String sortedContent = sample.stream().map(SortItem::toString).collect(Collectors.joining("\n"));
            sortedContent = sortedContent + "\n";

            // test single pass
            try (
                    BufferedReader r = Files.newBufferedReader(inputFile);
                    BufferedWriter w = Files.newBufferedWriter(outputFile)
            ) {
                CSVReader reader = DefaultCSVReader.builder(r).withoutHeader().build();
                CSVWriter writer = new DefaultCSVWriter(w);
                ExternalKwayMerge merger = new ExternalKwayMerge(binParams, sd);
                merger.singlePassMerge(reader, writer, 1L << 10, 128);
            }
            String resultSinglePass = Files.readString(outputFile);
            resultSinglePass = resultSinglePass.replace(System.lineSeparator(), "\n");
            Assertions.assertEquals(sortedContent, resultSinglePass);

            Files.writeString(outputFile, "", StandardOpenOption.TRUNCATE_EXISTING); // clear output
            // test double pass
            try (
                    BufferedReader r = Files.newBufferedReader(inputFile);
                    BufferedWriter w = Files.newBufferedWriter(outputFile)
            ) {
                CSVReader reader = DefaultCSVReader.builder(r).withoutHeader().build();
                CSVWriter writer = new DefaultCSVWriter(w);
                ExternalKwayMerge merger = new ExternalKwayMerge(binParams, sd);
                merger.doublePassMerge(reader, writer, 1L << 10, (int) Math.sqrt(sample.size()), 128);
            }
            String resultDoublePass = Files.readString(outputFile);
            resultDoublePass = resultDoublePass.replace(System.lineSeparator(), "\n");
            Assertions.assertEquals(sortedContent, resultDoublePass);
        }
    }

}
