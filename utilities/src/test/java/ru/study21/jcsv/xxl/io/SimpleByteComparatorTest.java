package ru.study21.jcsv.xxl.io;

import org.junit.jupiter.api.Test;
import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.CSVMeta;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleByteComparatorTest {

    @Test
    public void testInt() {
        SimpleByteComparator cmp = new SimpleByteComparator();

        ByteBuffer temp = ByteBuffer.allocate(4);
        byte[] arr1 = new byte[4];
        byte[] arr2 = new byte[4];
        temp.putInt(5).flip().get(arr1).clear();
        temp.putInt(3).flip().get(arr2).clear();

        assertTrue(cmp.compare(new CSVFileBinarizer.IntCTBS(), arr1, 0, arr2, 0) > 0);

        temp.putInt(1).flip().get(arr1).clear();
        assertTrue(cmp.compare(new CSVFileBinarizer.IntCTBS(), arr1, 0, arr2, 0) < 0);

        temp.putInt(1).flip().get(arr2).clear();
        assertEquals(0, cmp.compare(new CSVFileBinarizer.IntCTBS(), arr1, 0, arr2, 0));

        byte[] arrOffset = new byte[10];
        temp.putInt(2).flip().get(arrOffset, 4, 4).clear();
        assertTrue(cmp.compare(new CSVFileBinarizer.IntCTBS(), arr1, 0, arrOffset, 4) < 0);
    }

    // additional testing better be done using CSVFileBinarizer

    @Test
    public void testWithFile() throws IOException, BrokenContentsException {
        Path binPath = FileManager.createTempDirectory("SimpleByteComparatorTest").createTempFile("testWithFile");
        CSVReader reader = new CSVReader() {
            private final List<List<String>> contents = List.of(
                    List.of("1", "bbb", "555"),
                    List.of("2", "aaa", "333")
            );
            private final Iterator<List<String>> iter = contents.iterator();

            @Override
            public CSVMeta meta() {
                return null;
            }

            @Override
            public List<String> nextRow() {
                if (iter.hasNext()) {
                    return iter.next();
                }
                return List.of();
            }
        };

        CSVFileBinarizer.IntCTBS intCTBS = new CSVFileBinarizer.IntCTBS();
        CSVFileBinarizer.StringCTBS stringCTBS = new CSVFileBinarizer.StringCTBS(3, StandardCharsets.US_ASCII);
        CSVFileBinarizer.BigIntCTBS bigIntCTBS = new CSVFileBinarizer.BigIntCTBS(2);

        CSVFileBinarizer.binarize(reader, List.of(intCTBS, stringCTBS, bigIntCTBS), binPath);

        byte[] bytes = Files.readAllBytes(binPath);
        SimpleByteComparator cmp = new SimpleByteComparator();

        // bytes: 4 + 3 + 3 (!) per line
        assertEquals(1, cmp.parseInt(bytes, 0));
        assertEquals("bbb", cmp.parseString(bytes, 4, stringCTBS));
        assertEquals(new BigInteger("555"), cmp.parseBigInt(bytes, 7, bigIntCTBS.byteLength));
        assertEquals(2, cmp.parseInt(bytes, 10));
        assertEquals("aaa", cmp.parseString(bytes, 14, stringCTBS));
        assertEquals(new BigInteger("333"), cmp.parseBigInt(bytes, 17, bigIntCTBS.byteLength));

        assertTrue(cmp.compare(intCTBS, bytes, 0, bytes, 10) < 0);
        assertTrue(cmp.compare(stringCTBS, bytes, 4, bytes, 14) > 0);
        assertTrue(cmp.compare(bigIntCTBS, bytes, 7, bytes, 17) > 0);
    }

}
