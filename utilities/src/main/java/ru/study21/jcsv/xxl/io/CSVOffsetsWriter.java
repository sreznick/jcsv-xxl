package ru.study21.jcsv.xxl.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class CSVOffsetsWriter implements CSVWriter {
    private BufferedWriter _writer;
    private int _columnIndex;
    private char _separator;
    private long _rowNumber;

    private CSVOffsetsWriter(BufferedWriter writer, int columnIndex, char separator) {
        _writer = writer;
        _separator = separator;
        _columnIndex = columnIndex;
        _rowNumber = 1;
    }

    public void writeRow(List<String> row) {

    }

    public static Builder builder(BufferedWriter writer, int columnIndex) {
        return new Builder(writer, columnIndex);
    }

    public static class Builder {
        private BufferedWriter _writer;
        private int _columnIndex;
        private char _separator;

        public Builder(BufferedWriter writer, int columnIndex) {
            _writer = writer;
            _columnIndex = columnIndex;
            _separator = ',';
        }

        public Builder withSeparator(char c) {
            _separator = c;
            return this;
        }

        public CSVOffsetsWriter build() {
            return new CSVOffsetsWriter(_writer, _columnIndex, _separator);
        }
    }
}
