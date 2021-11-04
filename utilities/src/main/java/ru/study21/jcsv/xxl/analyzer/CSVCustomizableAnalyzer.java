package ru.study21.jcsv.xxl.analyzer;

import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.io.CSVReader;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CSVCustomizableAnalyzer {

    private final CSVReader _csvReader;
    private final List<Action<?>> actions;

    private CSVCustomizableAnalyzer(CSVReader csvReader, List<Action<?>> actions) {
        _csvReader = csvReader;
        this.actions = actions;
    }

    public static Builder builder(CSVReader csvReader) {
        return new Builder(csvReader);
    }

    public static class Builder {
        private final CSVReader _csvReader;
        private final List<Action<?>> actions;

        private Builder(CSVReader csvReader) {
            _csvReader = csvReader;
            actions = new ArrayList<>();
        }

        public Builder addAction(Action<?> action) {
            actions.add(action);
            return this;
        }

        public CSVCustomizableAnalyzer build() {
            return new CSVCustomizableAnalyzer(_csvReader, actions);
        }
    }

    public interface Action<R> {
        void acceptRow(List<String> row);

        R getResult();
    }

    public List<?> run() throws BrokenContentsException {
        while (true) {
            List<String> row = _csvReader.nextRow();
            if (row.size() == 0) {
                break;
            }
            if (row.size() != _csvReader.meta().size()) {
                throw new BrokenContentsException("illegal number of fields");
            }
            for (Action<?> action : actions) {
                action.acceptRow(row);
            }
        }
        return actions.stream().map(Action::getResult).toList();
    }

}
