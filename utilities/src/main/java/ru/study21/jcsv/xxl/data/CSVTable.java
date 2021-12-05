package ru.study21.jcsv.xxl.data;

import ru.study21.jcsv.xxl.common.*;
import ru.study21.jcsv.xxl.io.CSVReader;
import ru.study21.jcsv.xxl.io.DefaultCSVReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CSVTable {
    private final CSVMeta _meta;
    private final List<List<String>> _rows;

    private CSVTable(CSVMeta meta, List<List<String>> rows) {
        _meta = meta;
        _rows = rows;
    }

    public static CSVTable load(BufferedReader reader, CSVType csvType) throws IOException , BrokenContentsException {
        CSVReader csvReader = DefaultCSVReader.builder(reader).ofType(csvType).build();
        CSVMeta meta = csvReader.meta();
        List<List<String>> rows = new ArrayList<>();
        while (true) {
            List<String> row = csvReader.nextRow();
            if (row.size() == 0) {
                break;
            }
            rows.add(Collections.unmodifiableList(row));
        }

        return new CSVTable(meta, Collections.unmodifiableList(rows));
    }

    public CSVTable reallocate(List<Integer> index) {
        List<List<String>> rows = new ArrayList<>();
        for (int i: index) {
            rows.add(_rows.get(i));
        }

        return new CSVTable(_meta, Collections.unmodifiableList(rows));
    }

    public String cell(int row, int column) {
        return _rows.get(row).get(column);
    }

    public long cellAsLong(int row, int column) {
        return Long.parseLong(cell(row, column));
    }

    public BigInteger cellAsBig(int row, int column) {
        return new BigInteger(cell(row, column));
    }

    public int size() {
        return _rows.size();
    }

    public CSVMeta meta() {
        return _meta;
    }
}
