package ru.study21.jcsv.xxl.algorithms;

import org.junit.jupiter.api.Test;
import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.CSVType;
import ru.study21.jcsv.xxl.data.CSVTable;

import java.io.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MyersDiffTest {
    @Test
    public void testEditScriptByTwoColumns() throws IOException, BrokenContentsException {
        try (BufferedReader br1 = readerOf("""
                1,one
                2,two
                3,three
                4,four""");
             BufferedReader br2 = readerOf("""
                     1,one
                     3,three
                     4,four
                     2,two""")) {
            CSVTable firstTable = CSVTable.load(br1, new CSVType(false, ','));
            CSVTable secondTable = CSVTable.load(br2, new CSVType(false, ','));
            MyersDiff myersDiff = new MyersDiff(List.of(new MyersDiff.KeyInfo(0, SortDescription.KeyType.LONG),
                    new MyersDiff.KeyInfo(1, SortDescription.KeyType.STRING)));
            StringWriter stringWriter = new StringWriter();
            PrintWriter out = new PrintWriter(stringWriter);
            myersDiff.editScript(firstTable, secondTable, out);
            assertEquals("""
                            4a
                            2,two
                            .
                            2d
                            """,
                    stringWriter.toString().replace("\r", ""));
        }
    }

    @Test
    public void testEditScriptWithEmptyOutput() throws IOException, BrokenContentsException {
        try (BufferedReader br1 = readerOf("1,one\n2,two\n3,three\n4,four");
             BufferedReader br2 = readerOf("1,one\n2,two\n3,three\n4,four")) {
            CSVTable firstTable = CSVTable.load(br1, new CSVType(false, ','));
            CSVTable secondTable = CSVTable.load(br2, new CSVType(false, ','));
            MyersDiff myersDiff = new MyersDiff(List.of(new MyersDiff.KeyInfo(0, SortDescription.KeyType.LONG)));
            StringWriter stringWriter = new StringWriter();
            PrintWriter out = new PrintWriter(stringWriter);
            myersDiff.editScript(firstTable, secondTable, out);
            assertEquals("", stringWriter.toString().replace("\r", ""));
        }
    }

    @Test
    public void testEditScriptByOneColumn() throws IOException, BrokenContentsException {
        try (BufferedReader br1 = readerOf("""
                one
                two
                three
                four
                five
                six
                seven""");
             BufferedReader br2 = readerOf("""
                     seven
                     six
                     five
                     four
                     three""")) {
            CSVTable firstTable = CSVTable.load(br1, new CSVType(false, ','));
            CSVTable secondTable = CSVTable.load(br2, new CSVType(false, ','));
            MyersDiff myersDiff = new MyersDiff(List.of(new MyersDiff.KeyInfo(0, SortDescription.KeyType.STRING)));
            StringWriter stringWriter = new StringWriter();
            PrintWriter out = new PrintWriter(stringWriter);
            myersDiff.editScript(firstTable, secondTable, out);
            assertEquals("""
                            7a
                            three
                            .
                            7a
                            four
                            .
                            7a
                            five
                            .
                            7a
                            six
                            .
                            6d
                            5d
                            4d
                            3d
                            2d
                            1d
                            """,
                    stringWriter.toString().replace("\r", ""));
        }
    }

    @Test
    public void testDiff() throws IOException, BrokenContentsException {
        try (BufferedReader br1 = readerOf("""
                one
                two
                three
                four
                five
                six
                seven""");
             BufferedReader br2 = readerOf("""
                     seven
                     six
                     five
                     four
                     three""")) {
            CSVTable firstTable = CSVTable.load(br1, new CSVType(false, ','));
            CSVTable secondTable = CSVTable.load(br2, new CSVType(false, ','));
            MyersDiff myersDiff = new MyersDiff(List.of(new MyersDiff.KeyInfo(0, SortDescription.KeyType.STRING)));
            StringWriter stringWriter = new StringWriter();
            PrintWriter out = new PrintWriter(stringWriter);
            myersDiff.diff(firstTable, secondTable, out);
            assertEquals("""
                            DELETE one
                            DELETE two
                            DELETE three
                            DELETE four
                            DELETE five
                            DELETE six
                            TAKE seven
                            ADD six
                            ADD five
                            ADD four
                            ADD three
                            """,
                    stringWriter.toString().replace("\r", ""));
        }
    }

    private BufferedReader readerOf(String value) {
        return new BufferedReader(new StringReader(value));
    }

}
