package ru.study21.jcsv.xxl.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.CSVMeta;
import ru.study21.jcsv.xxl.common.CSVRow;
import ru.study21.jcsv.xxl.common.HeaderParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultCSVReaderTest {
    @Test
    void testSingleColumn() {
        String text = "name\nvalue1\nvalue2\n";

         try (BufferedReader reader = readerOf(text)) {
             DefaultCSVReader csvReader = DefaultCSVReader.builder(reader).withHeader().build();
             CSVMeta meta = csvReader.meta();

             assertNotNull(meta);
             assertTrue(meta.hasNames());
             assertEquals(1, meta.size());
             assertEquals("name", meta.columnName(0));

             CSVRow row = csvReader.nextRow();
             assertNotNull(row);
             assertEquals(1, row.size());
             assertEquals("value1", row.get(0));

             row = csvReader.nextRow();
             assertNotNull(row);
             assertEquals(1, row.size());
             assertEquals("value2", row.get(0));

             row = csvReader.nextRow();
             assertNotNull(row);
             assertEquals(0, row.size());
         } catch (IOException| BrokenContentsException e) {
                Assertions.fail("Unexpected " + e);
            }
    }

    @Test
    void testTwoColumns() {
        String text = "name1,name2\nvalue1,value2\nvalue3,value4";

        try (BufferedReader reader = readerOf(text)) {
            DefaultCSVReader csvReader = DefaultCSVReader.builder(reader).withHeader().withSeparator(',').build();
            CSVMeta meta = csvReader.meta();

            assertNotNull(meta);
            assertTrue(meta.hasNames());
            assertEquals(2, meta.size());
            assertEquals("name1", meta.columnName(0));
            assertEquals("name2", meta.columnName(1));

            CSVRow row = csvReader.nextRow();
            assertNotNull(row);
            assertEquals(2, row.size());
            assertEquals("value1", row.get(0));
            assertEquals("value2", row.get(1));
            assertEquals(0, row.offset());

            row = csvReader.nextRow();
            assertNotNull(row);
            System.out.println("RRR: " + row);
            assertEquals(2, row.size());
            assertEquals("value3", row.get(0));
            assertEquals("value4", row.get(1));
            assertEquals(1, row.offset());

            row = csvReader.nextRow();
            assertNotNull(row);
            assertEquals(0, row.size());
        } catch (IOException| BrokenContentsException e) {
            Assertions.fail("Unexpected " + e);
        }
    }

    private BufferedReader readerOf(String value) {
        return new BufferedReader(new StringReader(value));
    }
}
