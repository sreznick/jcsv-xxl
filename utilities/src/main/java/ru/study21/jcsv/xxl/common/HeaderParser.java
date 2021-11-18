package ru.study21.jcsv.xxl.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class HeaderParser implements HeaderSupplier {
    /*
     * Very simple and naive.
     *
     */

    private final RecordParser _recordParser;

    public HeaderParser(char separator) {
        _recordParser = new RecordParser(separator);
    }

    public CSVMeta parse(BufferedReader reader) throws IOException {
        List<String> values = _recordParser.parse(reader);

        return CSVMeta.withNames(values);
    }
}
