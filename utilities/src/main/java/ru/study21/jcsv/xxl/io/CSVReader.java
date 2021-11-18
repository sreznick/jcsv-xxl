package ru.study21.jcsv.xxl.io;

import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.CSVMeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public interface CSVReader {
    CSVMeta meta();
    List<String> nextRow() throws BrokenContentsException;

    /**
     * Be careful. It can be over-kill to call on huge files
     *
     * @return list of all rows
     */
    default List<List<String >> allRows() throws BrokenContentsException {
        List<List<String>> rows = new ArrayList<>();
        while (true) {
            List<String> row = nextRow();
            if (row.size() == 0) {
                break;
            }
            rows.add(Collections.unmodifiableList(row));
        }

        return Collections.unmodifiableList(rows);
    }
}
