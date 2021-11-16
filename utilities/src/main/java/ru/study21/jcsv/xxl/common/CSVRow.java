package ru.study21.jcsv.xxl.common;

import java.util.List;

public record CSVRow(List<String> row, long offset) {
    public int size() {
        return row().size();
    }

    public String get(int colIndex) {
        return row().get(colIndex);
    }
}
