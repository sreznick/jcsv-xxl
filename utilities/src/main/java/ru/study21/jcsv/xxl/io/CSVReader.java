package ru.study21.jcsv.xxl.io;

import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.CSVMeta;

import java.util.List;

public interface CSVReader {
    CSVMeta meta();
    List<String> nextRow() throws BrokenContentsException;
}
