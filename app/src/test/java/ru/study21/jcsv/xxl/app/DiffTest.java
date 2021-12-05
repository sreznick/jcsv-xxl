package ru.study21.jcsv.xxl.app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import ru.study21.jcsv.xxl.io.FileManager;

import java.io.*;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DiffTest {

    final PrintStream originalOut = System.out;
    final PrintStream originalErr = System.err;
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @BeforeEach
    public void setUpStreams() {
        out.reset();
        err.reset();
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void testDiff() throws IOException {
        FileManager fileManager = FileManager.createTempDirectory("test");
        Path file1 = fileManager.createTempFile(null);
        Path file2 = fileManager.createTempFile(null);

        try (BufferedWriter bw1 = new BufferedWriter(new FileWriter(file1.toFile()));
             BufferedWriter bw2 = new BufferedWriter(new FileWriter(file2.toFile()))) {
            bw1.write("""
                    name1,name2
                    1,one
                    2,two
                    3,three
                    4,four""");
            bw2.write("""
                    name1,name2
                    1,one
                    3,three
                    4,four
                    2,two""");
        }
        new CommandLine(new JCSVXXLApp()).execute("-h", "diff", "-e", file1.toString(), file2.toString());
        assertEquals("4a\n2,two\n.\n2d\n", out.toString().replace("\r", ""));
        fileManager.delete();
    }
}
