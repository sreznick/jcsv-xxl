package ru.study21.jcsv.xxl.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HeaderParser implements HeaderSupplier {
    /*
     * Very simple and naive.
     *
     * TODO: implement https://datatracker.ietf.org/doc/html/rfc4180
     */

    private final char _separator;

    public HeaderParser(char separator) {
        _separator = separator;
    }

    public CSVMeta parse(BufferedReader reader) throws IOException {
        final List<String> names = new ArrayList<>();
        var nameBuider = new StringBuilder();

        while (true) {
            int v = reader.read();
            if (v < 0) {
                names.add(nameBuider.toString());
                break;
            }
            char c = (char)v;

            if (c == _separator) {
                names.add(nameBuider.toString());
                nameBuider = new StringBuilder();
            } else {
                nameBuider.append(c);
            }
        }

        System.out.println("names: " + names);
        return new CSVMeta() {
            @Override
            public boolean hasHeader() {
                return true;
            }

            @Override
            public int columnsNumber() {
                return names.size();
            }

            @Override
            public String columnName(int i) {
                return names.get(i);
            }
        };
    }
}
