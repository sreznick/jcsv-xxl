package ru.study21.jcsv.xxl.io;

import org.junit.jupiter.api.Test;
import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.CSVMeta;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CuttingCSVReaderTest {

    @Test
    public void test() throws IOException, BrokenContentsException {
        Path file = FileManager.createTempDirectory("CuttingCSVReaderTest").createTempFile("test");
        String content = """
                col1,col2,col3
                1,a,x
                2,b,y
                3,c,z
                """;
        Files.writeString(file, content);

        CuttingCSVReader cuttingReader = new CuttingCSVReader(
                DefaultCSVReader.builder(Files.newBufferedReader(file)).withHeader().build(),
                List.of(2, 1, 0, 1)
        );

        assertEquals(CSVMeta.withNames(List.of("col3", "col2", "col1", "col2")), cuttingReader.meta());
        assertEquals(List.of("x", "a", "1", "a"), cuttingReader.nextRow());
        assertEquals(List.of("y", "b", "2", "b"), cuttingReader.nextRow());
        assertEquals(List.of("z", "c", "3", "c"), cuttingReader.nextRow());
        assertEquals(List.of(), cuttingReader.nextRow());
    }

}
