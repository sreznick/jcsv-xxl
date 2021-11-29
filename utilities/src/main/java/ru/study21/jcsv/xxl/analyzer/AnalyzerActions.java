package ru.study21.jcsv.xxl.analyzer;

import ru.study21.jcsv.xxl.common.CSVRow;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;

public class AnalyzerActions {
    public interface Action<R> {
        void acceptRow(CSVRow row);
        void finish();

        R getResult();
    }

    public static Action<BigInteger> sumAction(int colIndex) {
        return new Action<>() {
            private BigInteger sum = BigInteger.ZERO;

            @Override
            public void acceptRow(CSVRow row) {
                sum = sum.add(new BigInteger(row.get(colIndex)));
            }

            @Override
            public BigInteger getResult() {
                return sum;
            }

            @Override
            public void finish() {}
        };
    }

    public static Action<Double> averageAction(int colIndex) {
        return new Action<>() {
            private BigInteger sum = BigInteger.ZERO;
            private BigInteger count = BigInteger.ZERO;

            @Override
            public void acceptRow(CSVRow row) {
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

            @Override
            public void finish() {}
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
            public void acceptRow(CSVRow row) {
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

            @Override
            public void finish() {}
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
            public void acceptRow(CSVRow row) {
                delegate.acceptRow(row);
            }

            @Override
            public Integer getResult() {
                return delegate.getResult().get(0);
            }

            @Override
            public void finish() {}
        };
    }

    public static Action<Boolean> columnWithLineNumWriteAction(BufferedWriter writer, int colIndex) {
        return new Action<>() {
            boolean result = true;

            @Override
            public void acceptRow(CSVRow row) {
                try {
                    writer.write(String.join(",", String.valueOf(row.offset()), row.get(colIndex)) + "\n");
                } catch (IOException e) {
                    result = false;
                }
            }

            @Override
            public Boolean getResult() {
                return result;
            }

            @Override
            public void finish() {
                try {
                    writer.flush();
                } catch (IOException e) {
                    result = false;
                }
            }
        };
    }

    public static Action<ArrayList<CSVRow>> subRowAction(List<Integer> colIndexes) {
        return new Action<>() {
            ArrayList<CSVRow> result = new ArrayList<>();

            @Override
            public void acceptRow(CSVRow row) {
                List<String> newRow = new ArrayList<>();
                for (Integer colIndex : colIndexes) {
                    newRow.add(row.get(colIndex));
                }
                result.add(new CSVRow(newRow, row.offset()));
            }

            @Override
            public ArrayList<CSVRow> getResult() {
                return result;
            }

            @Override
            public void finish() {}
        };
    }
}
