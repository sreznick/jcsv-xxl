package ru.study21.jcsv.xxl.analyzer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.CSVMeta;
import ru.study21.jcsv.xxl.io.DefaultCSVReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CSVBasicAnalyzerTest {
    @Test
    void testSingleColumn() {
        String text = "name\nvalue1\nvalue2\n";

         try (BufferedReader reader = readerOf(text)) {
             DefaultCSVReader csvReader = DefaultCSVReader.builder(reader).withHeader().build();
             CSVBasicAnalyzer analyzer = new CSVBasicAnalyzer(csvReader);

             CSVSummary summary = analyzer.run();

             CSVMeta meta = summary.getMeta();

             assertNotNull(meta);
             assertTrue(meta.hasNames());
             assertEquals(1, meta.size());
             assertEquals("name", meta.columnName(0));

             assertEquals(2, summary.getNRows());
             ColumnSummary columnSummary = summary.getColumns().get(0);
             assertEquals(ColumnType.STRING, columnSummary.getType());
             assertEquals(6, columnSummary.getMaxStringSize());
             assertFalse(columnSummary.isHasEmpty());
         } catch (IOException| BrokenContentsException e) {
                Assertions.fail("Unexpected " + e);
            }
    }

    private BufferedReader readerOf(String value) {
        return new BufferedReader(new StringReader(value));
    }
}
