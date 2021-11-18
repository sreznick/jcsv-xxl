/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package ru.study21.jcsv.xxl.app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SummaryTest {

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
    void testSummary() {
        new CommandLine(new JCSVXXLApp()).execute("-h", "summary", "src/test/resources/sample_lf.csv");
        assertEquals("""
                --- Analyzing file sample_lf.csv ---
                File summary:
                	Rows total:	1
                Columns summary:
                	Name           | name1           | name2           |\s
                	Deduced type   | INTEGER         | INTEGER         |\s
                                
                """, out.toString().replace("\r", "")); // enforce LF
    }

}
