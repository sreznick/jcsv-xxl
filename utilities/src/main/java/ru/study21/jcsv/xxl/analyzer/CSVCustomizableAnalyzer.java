package ru.study21.jcsv.xxl.analyzer;

import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.io.CSVReader;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;

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

    // sample actions

    public static Action<BigInteger> sumAction(int colIndex) {
        return new Action<>() {
            private BigInteger sum = BigInteger.ZERO;

            @Override
            public void acceptRow(List<String> row) {
                sum = sum.add(new BigInteger(row.get(colIndex)));
            }

            @Override
            public BigInteger getResult() {
                return sum;
            }
        };
    }

    public static Action<Double> averageAction(int colIndex) {
        return new Action<>() {
            private BigInteger sum = BigInteger.ZERO;
            private BigInteger count = BigInteger.ZERO;

            @Override
            public void acceptRow(List<String> row) {
                sum = sum.add(new BigInteger(row.get(colIndex)));
                count = count.add(BigInteger.ONE);
            }

            @Override
            public Double getResult() {
                if (count.intValue() == 0) {
                    return 0d;
                }
                BigInteger[] divisionResult = sum.divideAndRemainder(count);
                // int division is more precise
                double result = divisionResult[0].doubleValue();
                result += divisionResult[1].doubleValue() / count.doubleValue();
                return result;
            }
        };
    }

    // TODO: improve generics (? extends T) - comparing is complicated
    public static <T> Action<List<T>> maxValuesAction(
            int colIndex,
            int nValues,
            Comparator<T> comparator,
            Function<String, T> parser
    ) {
        return new Action<>() {
            private final PriorityQueue<T> maxValues = new PriorityQueue<>(comparator);

            @Override
            public void acceptRow(List<String> row) {
                maxValues.add(parser.apply(row.get(colIndex)));
                if (maxValues.size() > nValues) {
                    maxValues.poll();
                }
            }

            @Override
            public List<T> getResult() {
                List<T> result = new ArrayList<>();
                while (!maxValues.isEmpty()) {
                    result.add(maxValues.poll());
                }
                Collections.reverse(result);
                return result;
            }
        };
    }

    public static Action<List<Integer>> maxValuesIntAction(int colIndex, int nValues) {
        return maxValuesAction(colIndex, nValues, Integer::compareTo, Integer::parseInt);
    }

    public static Action<Integer> maxIntAction(int colIndex) {
        return new Action<>() {
            private final Action<List<Integer>> delegate =
                    maxValuesIntAction(colIndex, 1);

            @Override
            public void acceptRow(List<String> row) {
                delegate.acceptRow(row);
            }

            @Override
            public Integer getResult() {
                return delegate.getResult().get(0);
            }
        };
    }

}
