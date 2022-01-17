package ru.study21.jcsv.xxl.io;

import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.CSVMeta;
import ru.study21.jcsv.xxl.common.Utility;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CuttingCSVReader implements CSVReader {

    private final CSVReader delegate;
    private final List<Integer> indices;

    private final CSVMeta meta;

    // can be used for a) cutting b) reordering
    public CuttingCSVReader(CSVReader delegate, List<Integer> leaveIndices) {
        this.delegate = delegate;
        this.indices = leaveIndices;

        if (delegate.meta().hasNames()) {
            ArrayList<String> newNames = IntStream
                    .range(0, delegate.meta().size())
                    .mapToObj(i -> delegate.meta().columnName(i))
                    .collect(Collectors.toCollection(ArrayList::new));
            meta = CSVMeta.withNames(Utility.slice(newNames, leaveIndices));
        } else {
            meta = CSVMeta.withoutNames(leaveIndices.size());
        }
    }

    // TODO: add building with names (erasure signature conflicts, use builder?)

    @Override
    public CSVMeta meta() {
        return meta;
    }

    @Override
    public List<String> nextRow() throws BrokenContentsException {
        List<String> row = delegate.nextRow();
        if(row.size() == 0) {
            return row;
        }
        return Utility.slice(row, indices);
    }
}
