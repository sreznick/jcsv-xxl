package ru.study21.jcsv.xxl.io;

import ru.study21.jcsv.xxl.common.BrokenContentsException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class DefaultCSVWriter implements CSVWriter {

    private final BufferedWriter writer;
    private int colCount;
    private final char separator;

    public DefaultCSVWriter(BufferedWriter writer, int colCount, char separator) {
        this.writer = writer;
        this.colCount = colCount;
        this.separator = separator;
    }

    public DefaultCSVWriter(BufferedWriter writer) {
        this(writer, -1, ',');
    }

    @Override
    public void write(List<String> row) throws BrokenContentsException {
        if (colCount == -1) {
            colCount = row.size();
        }
        if (colCount != row.size()) {
            throw new BrokenContentsException("row length differs from previous (" + colCount + " != " + row.size() + ")");
        }
        try {
            for (int i = 0; i < row.size(); i++) {
                writer.write(row.get(i));
                if (i != row.size() - 1) {
                    writer.write(separator);
                }
            }
            writer.newLine();
        } catch (IOException e) {
            // TODO FIXME
            throw new RuntimeException(e);
        }
    }

}
