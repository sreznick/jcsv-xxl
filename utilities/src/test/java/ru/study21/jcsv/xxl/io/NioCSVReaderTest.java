package ru.study21.jcsv.xxl.io;

import org.junit.jupiter.api.Test;
import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.CSVMeta;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NioCSVReaderTest {

    public final FileManager fm = FileManager.createTempDirectory("NioCSVReaderTest");
    public Path file = fm.createTempFile("file");

    public NioCSVReaderTest() throws IOException {
    }

    public void writeFile(String text) throws IOException {
        Files.writeString(file, text, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public NioCSVReader getReader(int cacheSize) throws IOException, BrokenContentsException {
        return new NioCSVReader(Files.newByteChannel(file, StandardOpenOption.READ), cacheSize);
    }

    public NioCSVReader getReaderWithHeader(int cacheSize) throws IOException, BrokenContentsException {
        return new NioCSVReader(Files.newByteChannel(file, StandardOpenOption.READ), cacheSize, true);
    }

    // tests based on RFC4180

    @Test
    public void test1_basic() throws IOException, BrokenContentsException {
        writeFile("""
                aaa,bbb,ccc
                zzz,yyy,xxx
                """);
        NioCSVReader reader = getReader(4);
        assertEquals(List.of("aaa", "bbb", "ccc"), reader.nextRow());
        assertEquals(List.of("zzz", "yyy", "xxx"), reader.nextRow());
        assertEquals(List.of(), reader.nextRow());
        assertEquals(CSVMeta.withoutNames(3), reader.meta());
    }

    @Test
    public void test2_eof() throws IOException, BrokenContentsException {
        writeFile("""
                aaa,bbb,ccc
                zzz,yyy,xxx""");
        NioCSVReader reader = getReader(5);
        assertEquals(List.of("aaa", "bbb", "ccc"), reader.nextRow());
        assertEquals(List.of("zzz", "yyy", "xxx"), reader.nextRow());
        assertEquals(List.of(), reader.nextRow());
        assertEquals(CSVMeta.withoutNames(3), reader.meta());
    }

    @Test
    public void test3_names() throws IOException, BrokenContentsException {
        writeFile("""
                field_name,field_name,field_name
                aaa,bbb,ccc
                zzz,yyy,xxx""");
        NioCSVReader reader = getReaderWithHeader(6);
        assertEquals(List.of("aaa", "bbb", "ccc"), reader.nextRow());
        assertEquals(List.of("zzz", "yyy", "xxx"), reader.nextRow());
        assertEquals(List.of(), reader.nextRow());
        assertEquals(CSVMeta.withNames(List.of("field_name", "field_name", "field_name")), reader.meta());
    }

    @Test
    public void test4_wrong() throws IOException, BrokenContentsException {
        // wrong column count
        writeFile("""
                aaa,bbb,ccc
                zzz,yyy
                """);
        NioCSVReader reader = getReader(7);
        assertEquals(List.of("aaa", "bbb", "ccc"), reader.nextRow());
        assertThrows(BrokenContentsException.class, reader::nextRow);

        // comma at line end
        writeFile("""
                aaa,bbb,ccc
                xxx,yyy,zzz,
                111,222,333""");
        reader = getReader(7);
        assertEquals(List.of("aaa", "bbb", "ccc"), reader.nextRow());
        assertThrows(BrokenContentsException.class, reader::nextRow);
    }

    @Test
    public void test5_quotes() throws IOException, BrokenContentsException {
        writeFile("""
                "aaa","bbb","ccc"
                zzz,yyy,xxx""");
        NioCSVReader reader = getReader(8);
        assertEquals(List.of("aaa", "bbb", "ccc"), reader.nextRow());
        assertEquals(List.of("zzz", "yyy", "xxx"), reader.nextRow());
        assertEquals(List.of(), reader.nextRow());
    }

    @Test
    public void test6_escapeBlock() throws IOException, BrokenContentsException {
        writeFile("""
                "aaa","b
                bb","ccc"
                zzz,yyy,xxx""");
        NioCSVReader reader = getReader(9);
        assertEquals(List.of("aaa", "b\nbb", "ccc"), reader.nextRow());
        assertEquals(List.of("zzz", "yyy", "xxx"), reader.nextRow());
        assertEquals(List.of(), reader.nextRow());
    }

    @Test
    public void test7_escapeQuote() throws IOException, BrokenContentsException {
        writeFile("""
                "aaa","b""bb","ccc"
                """);
        NioCSVReader reader = getReader(10);
        assertEquals(List.of("aaa", "b\"bb", "ccc"), reader.nextRow());
        assertEquals(List.of(), reader.nextRow());
    }

    @Test
    public void test8_caret() throws IOException, BrokenContentsException {
        writeFile("""
                aa,bb,cc
                xx,yy,zz\r
                pp,qq,rr
                """);
        NioCSVReader reader = getReader(11);
        assertEquals(List.of("aa", "bb", "cc"), reader.nextRow());
        assertEquals(List.of("xx", "yy", "zz"), reader.nextRow());
        assertEquals(List.of("pp", "qq", "rr"), reader.nextRow());
        assertEquals(List.of(), reader.nextRow());
    }

}
