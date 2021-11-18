package ru.study21.jcsv.xxl.algorithms;

import org.junit.jupiter.api.Test;
import ru.study21.jcsv.xxl.common.CSVType;
import ru.study21.jcsv.xxl.data.CSVTable;
import ru.study21.jcsv.xxl.algorithms.SortDescription.*;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

public class InMemorySorterTest {
    @Test
    void testSingleColumnString() {
        String text = "name\nvalue1\nvalue2\nother\nanother\nyet another";
        try (BufferedReader reader = readerOf(text)) {
            CSVTable table = CSVTable.load(reader, new CSVType(false, ','));
            InMemorySorter sorter = new InMemorySorter(SortDescription.of(KeyElement.asString(0)));
            CSVTable sortedTable = sorter.sorted(table);
            assertNotNull(sortedTable);
            assertEquals(table.size(), sortedTable.size());
            assertEquals("another", sortedTable.cell(0, 0));
            assertEquals("name", sortedTable.cell(1, 0));
            assertEquals("other", sortedTable.cell(2, 0));
            assertEquals("value1", sortedTable.cell(3, 0));
            assertEquals("value2", sortedTable.cell(4, 0));
            assertEquals("yet another", sortedTable.cell(5, 0));
        } catch (Exception unexpected) {
            fail(unexpected);
        }
    }

    @Test
    void testSingleColumnLong() {
        String text = "43\n0\n1\n-2\n567\n-40";
        try (BufferedReader reader = readerOf(text)) {
            CSVTable table = CSVTable.load(reader, new CSVType(false, ','));
            InMemorySorter sorter = new InMemorySorter(SortDescription.of(KeyElement.asLong(0)));
            CSVTable sortedTable = sorter.sorted(table);
            assertNotNull(sortedTable);
            assertEquals(table.size(), sortedTable.size());
            assertEquals(-40, sortedTable.cellAsLong(0, 0));
            assertEquals(-2, sortedTable.cellAsLong(1, 0));
            assertEquals(0, sortedTable.cellAsLong(2, 0));
            assertEquals(1, sortedTable.cellAsLong(3, 0));
            assertEquals(43, sortedTable.cellAsLong(4, 0));
            assertEquals(567, sortedTable.cellAsLong(5, 0));
        } catch (Exception unexpected) {
            fail(unexpected);
        }
    }

    private BufferedReader readerOf(String value) {
        return new BufferedReader(new StringReader(value));
    }
}
