package ru.study21.jcsv.xxl.io;

import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.CSVMeta;
import ru.study21.jcsv.xxl.common.CSVRow;

import java.util.List;

public interface CSVReader {
    CSVMeta meta();
    CSVRow nextRow() throws BrokenContentsException;
}
