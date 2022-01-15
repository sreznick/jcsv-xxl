package ru.study21.jcsv.xxl.io;

import ru.study21.jcsv.xxl.common.BrokenContentsException;

import java.io.IOException;
import java.util.List;

public interface CSVWriter {

    void write(List<String> row) throws BrokenContentsException;

}
