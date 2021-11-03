package ru.study21.jcsv.xxl.analyzer;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.study21.jcsv.xxl.common.CSVMeta;

import java.util.List;


@Builder
@Setter
@Getter
public class CSVSummary {
    private CSVMeta meta;
    private int nRows;
    private List<ColumnSummary> columns;
}
