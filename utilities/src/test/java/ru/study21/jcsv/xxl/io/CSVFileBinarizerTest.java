package ru.study21.jcsv.xxl.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.study21.jcsv.xxl.common.BrokenContentsException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        System.out.println(binFile.toString());

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

        System.out.println(binFile.toString());

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

        System.out.println(binFile.toString());

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

        System.out.println(binFile.toString());

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

        System.out.println(binFile.toString());

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

        System.out.println(binFile.toString());

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

        System.out.println(binFile.toString());

        CSVFileBinarizer.binarize(inReader, binParams, binFile);
        CSVFileBinarizer.debinarize(binFile, binParams, outWriter);

        outBufWriter.flush();
        outReader = DefaultCSVReader.builder(Files.newBufferedReader(csvOutFile)).withoutHeader().build();

        for (List<String> row : data) {
            assertEquals(row, outReader.nextRow());
        }
        assertEquals(0, outReader.nextRow().size());
    }

}
