package ru.study21.jcsv.xxl.analyzer;

import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.CSVRow;
import ru.study21.jcsv.xxl.io.CSVReader;
import ru.study21.jcsv.xxl.analyzer.AnalyzerActions;

import java.util.*;

public class CSVCustomizableAnalyzer {

    private final CSVReader _csvReader;
    private final List<AnalyzerActions.Action<?>> actions;

    private CSVCustomizableAnalyzer(CSVReader csvReader, List<AnalyzerActions.Action<?>> actions) {
        _csvReader = csvReader;
        this.actions = actions;
    }

    public static Builder builder(CSVReader csvReader) {
        return new Builder(csvReader);
    }

    public static class Builder {
        private final CSVReader _csvReader;
        private final List<AnalyzerActions.Action<?>> actions;

        private Builder(CSVReader csvReader) {
            _csvReader = csvReader;
            actions = new ArrayList<>();
        }

        public Builder addAction(AnalyzerActions.Action<?> action) {
            actions.add(action);
            return this;
        }

        public CSVCustomizableAnalyzer build() {
            return new CSVCustomizableAnalyzer(_csvReader, actions);
        }
    }

    public List<?> run() throws BrokenContentsException {
        while (true) {
            CSVRow row = _csvReader.nextRow();
            if (row.size() == 0) {
                break;
            }
            if (row.size() != _csvReader.meta().size()) {
                throw new BrokenContentsException("illegal number of fields");
            }
            for (AnalyzerActions.Action<?> action : actions) {
                action.acceptRow(row);
            }
        }
        for (AnalyzerActions.Action<?> action : actions) {
            action.finish();
        }
        return actions.stream().map(AnalyzerActions.Action::getResult).toList();
    }
}
