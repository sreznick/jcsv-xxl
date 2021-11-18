package ru.study21.jcsv.xxl.algorithms;

import ru.study21.jcsv.xxl.data.CSVTable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class InMemorySorter {
    SortDescription _sortDescription;

    public InMemorySorter(SortDescription sortDescription) {
        _sortDescription = sortDescription;
    }

    public CSVTable sorted(CSVTable table) {
        List<Integer> indices = IntStream.range(0, table.size())
                .boxed().sorted((a, b) -> {
                    if (Objects.equals(a, b)) {
                        return 0;
                    }
                    for (SortDescription.KeyElement ke : _sortDescription.keys()) {
                        int cmpResult;
                        switch (ke.keyType()) {
                            case LONG -> {
                                cmpResult = Long.compare(
                                        table.cellAsLong(a, ke.field()),
                                        table.cellAsLong(b, ke.field())
                                );
                            }
                            case STRING -> {
                                cmpResult = table.cell(a, ke.field()).compareTo(table.cell(b, ke.field()));
                            }
                            case BIG_INTEGER -> {
                                cmpResult = table.cellAsBig(a, ke.field()).compareTo(table.cellAsBig(b, ke.field()));
                            }
                            default -> throw new IllegalStateException("Internal error");
                        }

                        if (cmpResult != 0) {
                            return cmpResult;
                        }
                    }

                    return 0;
                }).collect(Collectors.toList());

        return table.reallocate(indices);
    }
}
