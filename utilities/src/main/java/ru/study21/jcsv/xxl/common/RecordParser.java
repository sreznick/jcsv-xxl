package ru.study21.jcsv.xxl.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class RecordParser implements RecordSupplier {
    /*
     * Very simple and naive.
     *
     * TODO: implement https://datatracker.ietf.org/doc/html/rfc4180
     */

    private final char _separator;

    public RecordParser(char separator) {
        _separator = separator;
    }

    private Result parseQuotedValue(Reader reader) throws IOException {
        StringBuilder result = new StringBuilder();

        while (true) {
            int v = reader.read();

            if (v < 0) {
                throw new IllegalArgumentException("Not closed quote");
            }

            char c = (char)v;
            if (c == '"') {
                v = reader.read();
                if (v < 0 || v == '\n' || v == '\r') {
                    return new Result(Reason.END_OF_LINE, result.toString());
                } else if (v == _separator) {
                    return new Result(Reason.END_OF_COLUMN, result.toString());
                } else if (v == '"') {
                    result.append('"');
                }
            } else {
                result.append(c);
            }
        }
    }

    private Result parseValue(Reader reader) throws IOException {
        int v = reader.read();

        if (v == '"') {
            return parseQuotedValue(reader);
        }

        StringBuilder result = new StringBuilder();
        while (true) {
            if (v < 0) {
                return new Result(Reason.END_OF_INPUT, result.toString());
            }

            char c = (char)v;
            if (c == _separator) {
                return new Result(Reason.END_OF_COLUMN, result.toString());

            } else if (c == '\n'/* || c == '\r'*/) { // TODO: fix: for CRLF it stops after \r, the next result is '\n'
                    return new Result(Reason.END_OF_LINE, result.toString());
            } else {
                result.append(c);
            }

            v = reader.read();
        }
    }

    public List<String> parse(BufferedReader reader) throws IOException {
        final List<String> values = new ArrayList<>();

        while (true) {
            Result result = parseValue(reader);
            if (result.reason == Reason.END_OF_INPUT && values.size() == 0 && result.value.isEmpty()) {
                break;
            }
            values.add(result.value());
            if (result.reason == Reason.END_OF_LINE || result.reason == Reason.END_OF_INPUT) {
                break;
            }
        }

        return values;
    }

    private enum Reason {
        END_OF_LINE,
        END_OF_COLUMN,
        END_OF_INPUT
    }
    private static record Result(Reason reason, String value) {
    }
}
