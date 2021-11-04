package ru.study21.jcsv.xxl.analyzer;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class ColumnSummary {
    private boolean hasEmpty;
    private ColumnType type;
    private int maxStringSize;
}
