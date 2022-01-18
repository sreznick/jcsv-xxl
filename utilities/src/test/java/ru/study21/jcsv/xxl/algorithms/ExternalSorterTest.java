package ru.study21.jcsv.xxl.algorithms;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.io.CSVFileBinarizer;
import ru.study21.jcsv.xxl.io.DefaultCSVReader;
import ru.study21.jcsv.xxl.io.FileManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ExternalSorterTest {

    @Test
    public void basicTest() throws IOException, BrokenContentsException {
        FileManager fm = FileManager.createTempDirectory("ExternalSorterTest_basic");
        Path inputFile = fm.createTempFile("inputFile");
        Path outputFile = fm.createTempFile("outputFile");

        String table = """
                8,a
                7,b
                6,c
                5,d
                4,e
                3,f
                2,g
                1,h
                """;
        Files.writeString(inputFile, table);

        // setup variables
        DefaultCSVReader inputReader = DefaultCSVReader.builder(Files.newBufferedReader(inputFile)).build();
        SortDescription sortDesc = SortDescription.of(new SortDescription.KeyElement(1, SortDescription.KeyType.STRING, SortDescription.Order.DESCENDING));
        CSVFileBinarizer.StringCTBS stringCTBS = new CSVFileBinarizer.StringCTBS(1, StandardCharsets.US_ASCII);

        ExternalSorter.sort_maximum(
                inputFile,
                inputReader,
                sortDesc,
                List.of(stringCTBS),
                outputFile,
                8192,
                128,
                4,
                16
        );

        String result = Files.readString(outputFile).replace("\r\n", "\n");

        Assertions.assertEquals("""
                1,h
                2,g
                3,f
                4,e
                5,d
                6,c
                7,b
                8,a
                """, result);
    }

    // runs for ~2 minutes
    @Test
    public void stressTest() throws IOException, BrokenContentsException {
        FileManager fm = FileManager.createTempDirectory("ExternalSorterTest_basic");
        Path inputFile = fm.createTempFile("inputFile");
        Path outputFile = fm.createTempFile("outputFile");

        Random random = new Random(5);

        for (int round = 0; round < 1000; round++) {
            // clear files
            Files.writeString(inputFile, "", StandardOpenOption.TRUNCATE_EXISTING);
            Files.writeString(outputFile, "", StandardOpenOption.TRUNCATE_EXISTING);
            // generate data
            List<Integer> data = IntStream.generate(random::nextInt).limit(10_000).boxed().collect(Collectors.toList());
            String table = data.stream().map(String::valueOf).collect(Collectors.joining("\n")) + "\n";
            Files.writeString(inputFile, table);
            Collections.sort(data);
            String expected = data.stream().map(String::valueOf).collect(Collectors.joining("\n")) + "\n";

            // setup variables
            DefaultCSVReader inputReader = DefaultCSVReader.builder(Files.newBufferedReader(inputFile)).build();
            SortDescription sortDesc = SortDescription.of(0, SortDescription.KeyType.LONG);
            CSVFileBinarizer.IntCTBS intCTBS = new CSVFileBinarizer.IntCTBS();

            ExternalSorter.sort_maximum(
                    inputFile,
                    inputReader,
                    sortDesc,
                    List.of(intCTBS),
                    outputFile,
                    8196,
                    256,
                    4,
                    16
            );

            String result = Files.readString(outputFile).replace("\r\n", "\n");
            Assertions.assertEquals(expected, result);
        }
    }

}
