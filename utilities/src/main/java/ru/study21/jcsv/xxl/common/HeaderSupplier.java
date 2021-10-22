package ru.study21.jcsv.xxl.common;

import java.io.BufferedReader;
import java.io.IOException;

interface HeaderSupplier {
    CSVMeta parse(BufferedReader reader) throws IOException, BrokenContentsException;
}
