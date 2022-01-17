package ru.study21.jcsv.xxl.io;

import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.CSVMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// appends row number to the LAST column (for performance concerns)
public class EnumeratorCSVReader implements CSVReader {

    private final CSVReader delegate;
    private final CSVMeta meta;

    private long cnt = 0;

    public EnumeratorCSVReader(CSVReader delegate) {
        this.delegate = delegate;
        if (delegate.meta().hasNames()) {
            List<String> names = delegate.meta().toRow();
            names.add(0, "");
            meta = CSVMeta.withNames(names);
        } else {
            meta = CSVMeta.withoutNames(delegate.meta().size());
        }
    }

    @Override
    public CSVMeta meta() {
        return meta;
    }

    @Override
    public List<String> nextRow() throws BrokenContentsException {
        List<String> row = new ArrayList<>(delegate.nextRow());
        if(row.size() == 0) {
            return row;
        }
        row.add(String.valueOf(cnt)); // performance concern
        cnt++;
        return row;
    }
}
