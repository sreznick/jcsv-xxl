/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package ru.study21.jcsv.xxl.common;

import java.util.List;

public class CSVMeta {
    private Either<Integer, List<String>> _data;

    private CSVMeta(Either<Integer, List<String>> data) {
        _data = data;
    }

    public static CSVMeta withoutNames(int size) {
        return new CSVMeta(Either.left(size));
    }

    public static CSVMeta withNames(List<String> names) {
        return new CSVMeta(Either.right(names));
    }

    public int size() {
        if (_data.isLeft()) {
            return _data.left();
        } else {
            return _data.right().size();
        }
    }

    public String columnName(int i) {
        if (i >= size()) {
            throw new IllegalArgumentException("incorrect index " + i);
        }

        if (_data.isLeft()) {
            return Integer.toString(i);
        } else {
            return _data.right().get(i);
        }
    }

    public boolean hasNames() {
        return _data.isRight();
    }
}
