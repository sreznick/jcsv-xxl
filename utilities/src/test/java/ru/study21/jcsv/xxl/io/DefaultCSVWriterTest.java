package ru.study21.jcsv.xxl.io;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import ru.study21.jcsv.xxl.common.BrokenContentsException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

public class DefaultCSVWriterTest {

    @Test
    public void testSingleColumn() throws BrokenContentsException, IOException {
        StringWriter strWriter = new StringWriter();
        BufferedWriter bufWriter = new BufferedWriter(strWriter);
        CSVWriter writer = new DefaultCSVWriter(bufWriter);

        writer.write(List.of("value"));
        bufWriter.flush();
        assertEquals("value" + System.lineSeparator(), strWriter.toString());

        writer.write(List.of("111"));
        writer.write(List.of("word"));
        bufWriter.flush();
        assertEquals("value" + System.lineSeparator()
                + "111" + System.lineSeparator()
                + "word" + System.lineSeparator(), strWriter.toString());
    }

    @Test
    public void testMultipleColumns() throws BrokenContentsException, IOException {
        StringWriter strWriter = new StringWriter();
        BufferedWriter bufWriter = new BufferedWriter(strWriter);
        CSVWriter writer = new DefaultCSVWriter(bufWriter, 2, ';');

        writer.write(List.of("name1", "name2"));
        bufWriter.flush();
        assertEquals("name1;name2" + System.lineSeparator(), strWriter.toString());

        writer.write(List.of("value11", "value12"));
        writer.write(List.of("value21", "value22"));
        bufWriter.flush();
        assertEquals("name1;name2" + System.lineSeparator()
                + "value11;value12" + System.lineSeparator()
                + "value21;value22" + System.lineSeparator(), strWriter.toString());
    }

    @Test
    public void testWrongColumnCount() {
        StringWriter strWriter = new StringWriter();
        BufferedWriter bufWriter = new BufferedWriter(strWriter);
        CSVWriter writer = new DefaultCSVWriter(bufWriter, 2, ',');

        assertThrows(BrokenContentsException.class, () -> writer.write(List.of("single")));
    }

}
