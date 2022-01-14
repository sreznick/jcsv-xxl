package ru.study21.jcsv.xxl.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.study21.jcsv.xxl.common.BrokenContentsException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CSVFileBinarizerTest {

    FileManager tempManager;
    Path csvInFile, binFile, csvOutFile;
    BufferedWriter inBufWriter, outBufWriter;
    CSVReader inReader, outReader;
    CSVWriter inWriter, outWriter;

    @BeforeEach
    public void setupIO() throws IOException {
        tempManager = FileManager.createTempDirectory("testing-binarizer");

        csvInFile = tempManager.createTempFile("in.csv");
        binFile = tempManager.createTempFile("res.bin");
        csvOutFile = tempManager.createTempFile("out.csv");

        inBufWriter = Files.newBufferedWriter(csvInFile);
        outBufWriter = Files.newBufferedWriter(csvOutFile);

//        fails before data is already written
//        inReader = DefaultCSVReader.builder(Files.newBufferedReader(csvInFile)).withoutHeader().build();
//        outReader = DefaultCSVReader.builder(Files.newBufferedReader(csvOutFile)).withoutHeader().build();
        inWriter = new DefaultCSVWriter(inBufWriter);
        outWriter = new DefaultCSVWriter(outBufWriter);
    }

    @AfterEach
    public void closeIO() throws IOException {
        tempManager.delete();
    }

    public void writeData(List<List<String>> data) throws BrokenContentsException, IOException {
        for (List<String> row : data) {
            inWriter.write(row);
        }
        inBufWriter.flush();
        inReader = DefaultCSVReader.builder(Files.newBufferedReader(csvInFile)).withoutHeader().build();
    }

    @Test
    public void testSingleColumnInt() throws BrokenContentsException, IOException {
        List<List<String>> data = List.of(
                List.of("1"),
                List.of("2"),
                List.of("3")
        );
        writeData(data);
        List<CSVFileBinarizer.ColumnTypeBinarizationParams> binParams = List.of(
                new CSVFileBinarizer.IntCTBS()
        );

        CSVFileBinarizer.binarize(inReader, binParams, binFile);
        CSVFileBinarizer.debinarize(binFile, binParams, outWriter);

        outBufWriter.flush();
        outReader = DefaultCSVReader.builder(Files.newBufferedReader(csvOutFile)).withoutHeader().build();

        for (List<String> row : data) {
            assertEquals(row, outReader.nextRow());
        }
        assertEquals(0, outReader.nextRow().size());
    }

    @Test
    public void testMultipleColumnInt() throws BrokenContentsException, IOException {
        List<List<String>> data = List.of(
                List.of("11", "12", "-13"),
                List.of("21", "-22", "23"),
                List.of("-31", "32", "33")
        );
        writeData(data);
        List<CSVFileBinarizer.ColumnTypeBinarizationParams> binParams = List.of(
                new CSVFileBinarizer.IntCTBS(),
                new CSVFileBinarizer.IntCTBS(),
                new CSVFileBinarizer.IntCTBS()
        );

        CSVFileBinarizer.binarize(inReader, binParams, binFile);
        CSVFileBinarizer.debinarize(binFile, binParams, outWriter);

        outBufWriter.flush();
        outReader = DefaultCSVReader.builder(Files.newBufferedReader(csvOutFile)).withoutHeader().build();

        for (List<String> row : data) {
            assertEquals(row, outReader.nextRow());
        }
        assertEquals(0, outReader.nextRow().size());
    }

    @Test
    public void testSingleColumnLong() throws BrokenContentsException, IOException {
        List<List<String>> data = List.of(
                List.of("1000000000000"),
                List.of("2000000000000"),
                List.of("3000000000000")
        );
        writeData(data);
        List<CSVFileBinarizer.ColumnTypeBinarizationParams> binParams = List.of(
                new CSVFileBinarizer.LongCTBS()
        );

        CSVFileBinarizer.binarize(inReader, binParams, binFile);
        CSVFileBinarizer.debinarize(binFile, binParams, outWriter);

        outBufWriter.flush();
        outReader = DefaultCSVReader.builder(Files.newBufferedReader(csvOutFile)).withoutHeader().build();

        for (List<String> row : data) {
            assertEquals(row, outReader.nextRow());
        }
        assertEquals(0, outReader.nextRow().size());
    }

    @Test
    public void testSingleColumnBigint() throws BrokenContentsException, IOException {
        BigInteger big = BigInteger.TWO.pow(123); // 16 bytes
        List<List<String>> data = List.of(
                List.of(big.toString()),
                List.of(big.add(BigInteger.ONE).toString()),
                List.of(big.subtract(BigInteger.ONE).toString())
        );
        writeData(data);
        List<CSVFileBinarizer.ColumnTypeBinarizationParams> binParams = List.of(
                new CSVFileBinarizer.BigIntCTBS(16)
        );

        CSVFileBinarizer.binarize(inReader, binParams, binFile);
        CSVFileBinarizer.debinarize(binFile, binParams, outWriter);

        outBufWriter.flush();
        outReader = DefaultCSVReader.builder(Files.newBufferedReader(csvOutFile)).withoutHeader().build();

        for (List<String> row : data) {
            assertEquals(row, outReader.nextRow());
        }
        assertEquals(0, outReader.nextRow().size());
    }

    @Test
    public void testSingleColumnString() throws BrokenContentsException, IOException {
        List<List<String>> data = List.of(
                List.of("aaa"),
                List.of("bbb"),
                List.of("ccc")
        );
        writeData(data);
        List<CSVFileBinarizer.ColumnTypeBinarizationParams> binParams = List.of(
                new CSVFileBinarizer.StringCTBS(3, StandardCharsets.US_ASCII)
        );

        CSVFileBinarizer.binarize(inReader, binParams, binFile);
        CSVFileBinarizer.debinarize(binFile, binParams, outWriter);

        outBufWriter.flush();
        outReader = DefaultCSVReader.builder(Files.newBufferedReader(csvOutFile)).withoutHeader().build();

        for (List<String> row : data) {
            assertEquals(row, outReader.nextRow());
        }
        assertEquals(0, outReader.nextRow().size());
    }

    @Test
    public void testSingleColumnStringCuts() throws BrokenContentsException, IOException {
        List<List<String>> data = List.of(
                List.of("aaabbbccc")
        );
        writeData(data);
        List<CSVFileBinarizer.ColumnTypeBinarizationParams> binParams = List.of(
                new CSVFileBinarizer.StringCTBS(3, StandardCharsets.US_ASCII)
        );

        CSVFileBinarizer.binarize(inReader, binParams, binFile);
        CSVFileBinarizer.debinarize(binFile, binParams, outWriter);

        outBufWriter.flush();
        outReader = DefaultCSVReader.builder(Files.newBufferedReader(csvOutFile)).withoutHeader().build();

        assertEquals(List.of("aaa"), outReader.nextRow());
        assertEquals(0, outReader.nextRow().size());
    }

    @Test
    public void testMultipleColumnMultipleType() throws BrokenContentsException, IOException {
        List<List<String>> data = List.of(
                List.of("1", "11", "111", "aaa"),
                List.of("2", "22", "222", "bbb"),
                List.of("3", "33", "333", "ccc")
        );
        writeData(data);
        List<CSVFileBinarizer.ColumnTypeBinarizationParams> binParams = List.of(
                new CSVFileBinarizer.IntCTBS(),
                new CSVFileBinarizer.LongCTBS(),
                new CSVFileBinarizer.BigIntCTBS(2), // actually fits in a short
                new CSVFileBinarizer.StringCTBS(3, StandardCharsets.US_ASCII)
        );

        CSVFileBinarizer.binarize(inReader, binParams, binFile);
        CSVFileBinarizer.debinarize(binFile, binParams, outWriter);

        outBufWriter.flush();
        outReader = DefaultCSVReader.builder(Files.newBufferedReader(csvOutFile)).withoutHeader().build();

        for (List<String> row : data) {
            assertEquals(row, outReader.nextRow());
        }
        assertEquals(0, outReader.nextRow().size());
    }

    @Test
    public void testSingleColumnIntValues() throws BrokenContentsException, IOException {
        List<List<String>> data = IntStream.range(-1024, 1024)
                .mapToObj(i -> List.of(String.valueOf(i)))
                .collect(Collectors.toList());
        writeData(data);
        List<CSVFileBinarizer.ColumnTypeBinarizationParams> binParams = List.of(
                new CSVFileBinarizer.IntCTBS()
        );

        CSVFileBinarizer.binarize(inReader, binParams, binFile);
        CSVFileBinarizer.debinarize(binFile, binParams, outWriter);

        outBufWriter.flush();
        outReader = DefaultCSVReader.builder(Files.newBufferedReader(csvOutFile)).withoutHeader().build();

        for (List<String> row : data) {
            assertEquals(row, outReader.nextRow());
        }
        assertEquals(0, outReader.nextRow().size());
    }

    @Test
    public void testSingleColumnBigintValues() throws BrokenContentsException, IOException {
        List<List<String>> data = IntStream.range(-1024, 1024)
                .mapToObj(i -> List.of(String.valueOf(i)))
                .collect(Collectors.toList());
        writeData(data);
        List<CSVFileBinarizer.ColumnTypeBinarizationParams> binParams = List.of(
                new CSVFileBinarizer.BigIntCTBS(2)
        );

        CSVFileBinarizer.binarize(inReader, binParams, binFile);
        CSVFileBinarizer.debinarize(binFile, binParams, outWriter);

        outBufWriter.flush();
        outReader = DefaultCSVReader.builder(Files.newBufferedReader(csvOutFile)).withoutHeader().build();

        for (List<String> row : data) {
            assertEquals(row, outReader.nextRow());
        }
        assertEquals(0, outReader.nextRow().size());
    }

    @Test
    public void testSingleColumnBigintTooLarge() throws BrokenContentsException, IOException {
        List<List<String>> data = List.of(
                List.of("999999999999999999")
        );
        writeData(data);
        List<CSVFileBinarizer.ColumnTypeBinarizationParams> binParams = List.of(
                new CSVFileBinarizer.BigIntCTBS(3)
        );

        assertThrows(IllegalArgumentException.class, () -> CSVFileBinarizer.binarize(inReader, binParams, binFile));
    }

    public static class MyRandom {
        private final Random r;

        public MyRandom(int seed) {
            r = new Random(seed);
        }


        public int randomInt() {
            return r.nextInt();
        }

        private final ByteBuffer tmp = ByteBuffer.allocate(8);

        public long randomLong() {
            return tmp.clear().putInt(randomInt()).putInt(randomInt()).flip().getLong();
        }

        // always of length <= 16 for our purposes
        public BigInteger randomBigInt() {
            long positiveLong;
            do {
                positiveLong = randomLong();
            } while (positiveLong < 0);
            return new BigInteger(randomLong() + "" + positiveLong);
        }

        public char randomChar() {
            return (char) ('a' + r.nextInt('z' - 'a'));
        }

        public String randomString(int length) {
            return IntStream.generate(this::randomChar).mapToObj(Character::toString).limit(length).collect(Collectors.joining());
        }
    }

    @Test
    public void stress() throws BrokenContentsException, IOException {
        MyRandom r = new MyRandom(5);

        CSVFileBinarizer.IntCTBS intCTBS = new CSVFileBinarizer.IntCTBS();
        CSVFileBinarizer.LongCTBS longCTBS = new CSVFileBinarizer.LongCTBS();
        CSVFileBinarizer.BigIntCTBS bigIntCTBS = new CSVFileBinarizer.BigIntCTBS(16);
        CSVFileBinarizer.StringCTBS stringCTBS = new CSVFileBinarizer.StringCTBS(10, StandardCharsets.US_ASCII);
        List<CSVFileBinarizer.ColumnTypeBinarizationParams> binParams = List.of(
                intCTBS, longCTBS, bigIntCTBS, stringCTBS, intCTBS
        );

        for (int round = 0; round < 1000; round++) {
            setupIO();

            List<List<String>> table = new ArrayList<>();
            for (int t = 0; t < 100; t++) {
                table.add(List.of(
                        String.valueOf(r.randomInt()),
                        String.valueOf(r.randomLong()),
                        r.randomBigInt().toString(),
                        r.randomString(10),
                        String.valueOf(r.randomInt())
                ));
            }
            writeData(table);

            CSVFileBinarizer.binarize(inReader, binParams, binFile);
            CSVFileBinarizer.debinarize(binFile, binParams, outWriter);

            outBufWriter.flush();
            outReader = DefaultCSVReader.builder(Files.newBufferedReader(csvOutFile)).withoutHeader().build();

            for (List<String> row : table) {
                assertEquals(row, outReader.nextRow());
            }
            assertEquals(0, outReader.nextRow().size());

            // clear files
            Files.writeString(csvInFile, "", StandardOpenOption.TRUNCATE_EXISTING);
            Files.write(binFile, new byte[]{}, StandardOpenOption.TRUNCATE_EXISTING);
            Files.writeString(csvOutFile, "", StandardOpenOption.TRUNCATE_EXISTING);
        }
    }


}
