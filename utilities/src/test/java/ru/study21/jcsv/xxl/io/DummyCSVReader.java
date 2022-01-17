package ru.study21.jcsv.xxl.io;

import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.CSVMeta;

import java.util.Iterator;
import java.util.List;

public class DummyCSVReader implements CSVReader {

    private final Iterator<List<String>> tableIter;
    private final CSVMeta meta;

    public DummyCSVReader(List<List<String>> table, List<String> meta) {
        if (table.size() == 0) {
            throw new IllegalArgumentException("come on");
        }
        this.tableIter = table.iterator();
        this.meta = (meta == null ? CSVMeta.withoutNames(table.get(0).size()) : CSVMeta.withNames(meta));
    }

    public DummyCSVReader(List<List<String>> table) {
        this(table, null);
    }

    @Override
    public CSVMeta meta() {
        return meta;
    }

    @Override
    public List<String> nextRow() {
        if (tableIter.hasNext()) {
            return tableIter.next();
        }
        return List.of();
    }
}
