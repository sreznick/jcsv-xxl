package ru.study21.jcsv.xxl.analyzer;

import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.io.CSVReader;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class CSVBasicAnalyzer {
    CSVReader _csvReader;

    public CSVBasicAnalyzer(CSVReader csvReader) {
        _csvReader = csvReader;
    }

    public CSVSummary run() throws BrokenContentsException {
        List<ColumnSummary> columns = new ArrayList<>();
        for (int i = 0; i < _csvReader.meta().size(); ++i) {
            ColumnSummary summary = ColumnSummary.builder()
                    .hasEmpty(false)
                    .maxStringSize(0)
                    .type(ColumnType.INTEGER)
                    .build();

            columns.add(summary);
        }
        CSVSummary summary = new CSVSummary(_csvReader.meta(), 0, columns);
        while (true) {
            List<String> row = _csvReader.nextRow();
            if (row.size() == 0) {
                break;
            }
            if (row.size() != _csvReader.meta().size()) {
                throw new BrokenContentsException("illegal number of fields");
            }
            for (int i=0; i < row.size(); ++i) {
                ColumnSummary columnSummary = summary.getColumns().get(i);
                if (row.get(i).isEmpty()) {
                    columnSummary.setHasEmpty(true);
                }
                String columnValue = row.get(i);
                columnSummary.setType(probeType(columnValue, columnSummary.getType()));
                columnSummary.setMaxStringSize(Math.max(columnSummary.getMaxStringSize(), columnValue.length()));
            }

            summary.setNRows(summary.getNRows() + 1);
        }

        return summary;
    }

    private static ColumnType probeType(String value, ColumnType baseType) {
        if (baseType == ColumnType.INTEGER) {
            try {
                Integer.parseInt(value);
                return baseType;
            } catch (NumberFormatException e) {
                return probeType(value, ColumnType.LONG);
            }
        }

        if (baseType == ColumnType.LONG) {
            try {
                Long.parseLong(value);
                return baseType;
            } catch (NumberFormatException e) {
                return probeType(value, ColumnType.BIGINT);
            }
        }


        if (baseType == ColumnType.BIGINT) {
            try {
                new BigInteger(value);
                return baseType;
            } catch (NumberFormatException e) {
            }
        }

        return ColumnType.STRING;
    }
}
