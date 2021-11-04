package ru.study21.jcsv.xxl.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

public interface RecordSupplier {
    List<String> parse(BufferedReader reader) throws IOException, BrokenContentsException;
}
