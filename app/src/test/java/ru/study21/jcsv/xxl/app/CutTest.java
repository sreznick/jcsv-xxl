package ru.study21.jcsv.xxl.app;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CutTest {

    @Test
    void testCut() throws IOException {
        String path = "src/test/resources/sample_lf.csv";
        String output = "src/test/resources/output.csv";
        int code = new CommandLine(new JCSVXXLApp()).execute("-h", "cut", "-o", output, "-l", "1", "-l", "0", "-i", path);
        Assertions.assertEquals(0, code);
        Assertions.assertEquals("""
                name2,name1
                2,1
                """, Files.readString(Path.of(output)).replace("\r", ""));
    }

}
