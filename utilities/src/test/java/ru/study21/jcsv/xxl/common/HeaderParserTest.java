package ru.study21.jcsv.xxl.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HeaderParserTest {
    private record Pair<T>(T first, T second) {
        static <T> Pair<T> of(T first, T second) {
            return new Pair<>(first, second);
        }
    }

    @Test
    void testSingleColumn() {
        HeaderParser parser = new HeaderParser(',');
        List<String> names = List.of("name", "hello");

        names.forEach(name -> {
            try (BufferedReader reader = readerOf(name)) {
                CSVMeta meta = parser.parse(reader);

                assertNotNull(meta);
                assertTrue(meta.hasNames());
                assertEquals(1, meta.size());
                assertEquals(name, meta.columnName(0));
            } catch (IOException e) {
                Assertions.fail("Unexpected " + e);
            }
        });
    }

    @Test
    void testSingleColumnContinued() {
        HeaderParser parser = new HeaderParser(',');
        List<String> names = List.of("name", "hello");

        names.forEach(name -> {
            // TODO: enable \r when CRLF bug is fixed
            List.of('\n'/*, '\r'*/).forEach(nl -> {
                try (BufferedReader reader = readerOf(name + nl + "123")) {
                    CSVMeta meta = parser.parse(reader);

                    assertNotNull(meta);
                    assertTrue(meta.hasNames());
                    assertEquals(1, meta.size());
                    assertEquals(name, meta.columnName(0));

                    assertEquals('1', reader.read());
                } catch (IOException e) {
                    Assertions.fail("Unexpected " + e);
                }
            });
        });
    }

    @Test
    void testSingleColumnQuoted() {
        HeaderParser parser = new HeaderParser(',');
        List<Pair<String>> cases = List.of(
                Pair.of("name", "name"),
                Pair.of("\"\"", ""),
                Pair.of("\"\"\"\"", "\""),
                Pair.of("\"\n\"", "\n")
        );

        cases.forEach(c -> {
            String name = c.first;
            try (BufferedReader reader = readerOf(name)) {
                CSVMeta meta = parser.parse(reader);

                assertNotNull(meta);
                assertTrue(meta.hasNames());
                assertEquals(1, meta.size());
                assertEquals(c.second, meta.columnName(0));
            } catch (IOException e) {
                Assertions.fail("Unexpected " + e);
            }
        });
    }

    @Test
    void testColumns2() {
        List<String> names = List.of("name1,name2", "name1,");
        String separators = ",:\t+=-";

        separators.chars().forEach(v -> {
            char sep = (char)v;
            HeaderParser parser = new HeaderParser(sep);
            try (BufferedReader reader = readerOf(String.format("%s%c%s", "name1", sep, "name2"))) {
                CSVMeta meta = parser.parse(reader);

                assertNotNull(meta);
                assertTrue(meta.hasNames());
                assertEquals(2, meta.size());
                assertEquals("name1", meta.columnName(0));
                assertEquals("name2", meta.columnName(1));
            } catch (IOException e) {
                Assertions.fail("Unexpected " + e);
            }
        });
    }

    private BufferedReader readerOf(String value) {
        return new BufferedReader(new StringReader(value));
    }
}
