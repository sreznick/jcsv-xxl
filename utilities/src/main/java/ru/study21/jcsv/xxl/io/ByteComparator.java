package ru.study21.jcsv.xxl.io;

// compare serialized values for sorting binary files
// heavily depends on CSVFileBinarizer implementation
public interface ByteComparator {
    int compare(CSVFileBinarizer.ColumnTypeBinarizationParams type,
                byte[] arr1, int offset1,
                byte[] arr2, int offset2);
}
