package ru.study21.jcsv.xxl.algorithms;

import org.junit.jupiter.api.Test;
import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.io.CSVReader;
import ru.study21.jcsv.xxl.io.DefaultCSVReader;
import ru.study21.jcsv.xxl.io.FileManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BatchSorterCSVReaderTest {

    @Test
    public void test() throws IOException, BrokenContentsException {
        Path path = FileManager.createTempDirectory("BatchSorterCSVReaderTest").createTempFile("");
        Files.writeString(path, "6\n5\n4\n3\n2\n1");

        CSVReader delegate = new DefaultCSVReader.Builder(
                Files.newBufferedReader(path)
        ).withoutHeader().build();

        ExternalKwayMerge.BatchSorterCSVReader reader = new ExternalKwayMerge.BatchSorterCSVReader(
                delegate,
                3,
                SortDescription.of(SortDescription.KeyElement.asLong(0)).toRowComparator()
        );

        for(String t : List.of("4", "5", "6", "1", "2", "3")) {
            assertEquals(List.of(t), reader.nextRow());
        }

    }

}
