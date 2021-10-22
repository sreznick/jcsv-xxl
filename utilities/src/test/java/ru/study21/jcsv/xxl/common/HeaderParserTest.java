package ru.study21.jcsv.xxl.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HeaderParserTest {
    @Test
    void testSingleColumn() {
        HeaderParser parser = new HeaderParser(',');
        List<String> names = List.of("name", "hello");

        names.forEach(name -> {
            try (BufferedReader reader = readerOf(name)) {
                CSVMeta meta = parser.parse(reader);

                assertNotNull(meta);
                assertTrue(meta.hasHeader());
                assertEquals(1, meta.columnsNumber());
                assertEquals(name, meta.columnName(0));
            } catch (IOException e) {
                Assertions.fail("Unexpected " + e);
            }
        });
    }

    private BufferedReader readerOf(String value) {
        return new BufferedReader(new StringReader(value));
    }
}
