package ru.study21.jcsv.xxl.io;

import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.CSVMeta;
import ru.study21.jcsv.xxl.common.CSVRow;

public interface CSVReader {
    CSVMeta meta();
    CSVRow nextRow() throws BrokenContentsException;
}
