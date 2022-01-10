package ru.study21.jcsv.xxl.algorithms;

import ru.study21.jcsv.xxl.algorithms.SortDescription.KeyType;
import ru.study21.jcsv.xxl.data.CSVTable;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class MyersDiff {

    private List<Point> _shortestPath;
    private final List<KeyInfo> _keys;
    private List<Operation> _operations;

    public MyersDiff(List<KeyInfo> _keys) {
        this._keys = _keys;
    }

    public record KeyInfo(int field, KeyType keyType) {
    }

    private record Point(int x, int y) {
    }

    private record Interval(int start, int end) {
        public int size() {
            return end - start;
        }
    }

    private record Operation(OperationType type, String content) {
    }

    private void addPointsBetweenTwoPointsToList(int x, int y, Point prevPoint, List<Point> result) {
        int xNext = prevPoint.x, yNext = prevPoint.y;

        while (xNext != x || yNext != y) {
            if (xNext < x && yNext < y) {
                xNext++;
                yNext++;
            } else if (xNext < x) {
                xNext++;
            } else {
                yNext++;
            }
            result.add(new Point(xNext, yNext));
        }
    }

    private boolean equalsLine(CSVTable firstCSVTable, CSVTable secondCSVTable,
                               int firstLineNumber, int secondLineNumber, List<KeyInfo> keys) {
        for (KeyInfo key : keys) {
            switch (key.keyType()) {
                case STRING -> {
                    if (!firstCSVTable.cell(firstLineNumber, key.field)
                            .equals(secondCSVTable.cell(secondLineNumber, key.field))) {
                        return false;
                    }
                }
                case LONG -> {
                    if (firstCSVTable.cellAsLong(firstLineNumber, key.field) !=
                            secondCSVTable.cellAsLong(secondLineNumber, key.field)) {
                        return false;
                    }
                }
                case BIG_INTEGER -> {
                    if (!firstCSVTable.cellAsBig(firstLineNumber, key.field)
                            .equals(secondCSVTable.cellAsBig(secondLineNumber, key.field))) {
                        return false;
                    }
                }
                default -> throw new IllegalArgumentException("This cell type is not supported.");
            }
        }
        return true;
    }

    private List<Point> getMiddleSnake(CSVTable firstCSVTable, CSVTable secondCSVTable, Interval firstInterval,
                                       Interval secondInterval) {
        int firstIntervalSize = firstInterval.size();
        int secondIntervalSize = secondInterval.size();
        int delta = firstIntervalSize - secondIntervalSize;

        int[] forward = new int[2 * Math.min(firstIntervalSize, secondIntervalSize) + 2];
        int[] backward = new int[2 * Math.min(firstIntervalSize, secondIntervalSize) + 2];
        backward[0] = firstIntervalSize;
        backward[(delta - 1 + backward.length) % backward.length] = firstIntervalSize;

        for (int d = 0; d <= Math.ceil((firstIntervalSize + secondIntervalSize) / 2.0); d++) {
            for (int k = -d; k <= d; k += 2) {
                boolean down;

                final int firstIndexInArray = (k - 1 + forward.length) % forward.length;
                final int secondIndexInArray = (k + 1 + forward.length) % forward.length;

                if (k == -d) {
                    down = true;
                } else if (k == d) {
                    down = false;
                } else {
                    down = forward[firstIndexInArray] < forward[secondIndexInArray];
                }

                int kPrev = down ? secondIndexInArray : firstIndexInArray;

                int xStart = forward[kPrev];
                int yStart = xStart - (down ? k + 1 : k - 1);

                int xMid = down ? xStart : xStart + 1;
                int yMid = xMid - k;

                int xEnd = xMid;
                int yEnd = yMid;

                while (xEnd < firstIntervalSize && yEnd < secondIntervalSize &&
                        equalsLine(firstCSVTable, secondCSVTable, xEnd + firstInterval.start,
                                yEnd + secondInterval.start, _keys)) {
                    xEnd++;
                    yEnd++;
                }

                forward[(k + forward.length) % forward.length] = xEnd;

                if (Math.abs(delta) % 2 == 1 && (k >= delta - (d - 1) && k <= delta + (d - 1)) &&
                        backward[(k + backward.length) % backward.length] <= xEnd) {
                    if (yStart < 0) {
                        yStart++;
                    }

                    List<Point> result = new ArrayList<>();

                    result.add(new Point(xStart + firstInterval.start, yStart + secondInterval.start));
                    result.add(new Point(xMid + firstInterval.start, yMid + secondInterval.start));
                    result.add(new Point(xEnd + firstInterval.start, yEnd + secondInterval.start));

                    return result;
                }
            }

            for (int k = d + delta; k >= -d + delta; k -= 2) {
                boolean up;

                int firstIndexInArray = (k - 1 + backward.length) % backward.length;
                int secondIndexInArray = (k + 1 + backward.length) % backward.length;

                if (k == d + delta) {
                    up = true;
                } else if (k == -d + delta) {
                    up = false;
                } else {
                    up = backward[firstIndexInArray] < backward[secondIndexInArray];
                }

                int kPrev = up ? firstIndexInArray : secondIndexInArray;

                int xStart = backward[kPrev];
                int yStart = xStart - (up ? k - 1 : k + 1);

                int xMid = up ? xStart : xStart - 1;
                int yMid = xMid - k;

                int xEnd = xMid;
                int yEnd = yMid;

                while (xEnd > 0 && yEnd > 0 &&
                        equalsLine(firstCSVTable, secondCSVTable, xEnd - 1 + firstInterval.start,
                                yEnd - 1 + secondInterval.start, _keys)) {
                    xEnd--;
                    yEnd--;
                }

                backward[(k + backward.length) % backward.length] = xEnd;

                if (delta % 2 == 0 && (k >= -d && k <= d) && xEnd <= forward[(k + forward.length) % forward.length]) {
                    if (yStart > secondIntervalSize) {
                        yStart--;
                    }

                    List<Point> result = new ArrayList<>();

                    result.add(new Point(xEnd + firstInterval.start, yEnd + secondInterval.start));
                    result.add(new Point(xMid + firstInterval.start, yMid + secondInterval.start));
                    result.add(new Point(xStart + firstInterval.start, yStart + secondInterval.start));

                    return result;
                }
            }
        }
        return new ArrayList<>();
    }

    private List<Point> linearSpaceDiff(CSVTable firstCSVTable, CSVTable secondCSVTable, Interval firstInterval,
                                        Interval secondInterval) {
        if (firstInterval.size() == 0 || secondInterval.size() == 0) {
            return new ArrayList<>();
        }

        List<Point> middleSnake = getMiddleSnake(firstCSVTable, secondCSVTable, firstInterval, secondInterval);
        if (middleSnake.get(0).x == 0 && middleSnake.get(0).y == 0 &&
                middleSnake.get(middleSnake.size() - 1).x == firstInterval.size() &&
                middleSnake.get(middleSnake.size() - 1).y == secondInterval.size()) {
            return middleSnake;
        }

        List<Point> beforeMiddleSnake = linearSpaceDiff(firstCSVTable, secondCSVTable,
                new Interval(firstInterval.start, middleSnake.get(0).x),
                new Interval(secondInterval.start, middleSnake.get(0).y));

        List<Point> afterMiddleSnake = linearSpaceDiff(firstCSVTable, secondCSVTable,
                new Interval(middleSnake.get(middleSnake.size() - 1).x, firstInterval.end),
                new Interval(middleSnake.get(middleSnake.size() - 1).y, secondInterval.end));

        beforeMiddleSnake.addAll(middleSnake);
        beforeMiddleSnake.addAll(afterMiddleSnake);

        return beforeMiddleSnake;
    }

    private void init(CSVTable firstCSVTable, CSVTable secondCSVTable) {
        List<Point> result = linearSpaceDiff(firstCSVTable, secondCSVTable, new Interval(0, firstCSVTable.size()),
                new Interval(0, secondCSVTable.size()));
        List<Point> answer = new ArrayList<>();
        answer.add(new Point(0, 0));
        for (Point point : result) {
            addPointsBetweenTwoPointsToList(point.x, point.y, answer.get(answer.size() - 1), answer);
        }
        addPointsBetweenTwoPointsToList(firstCSVTable.size(), secondCSVTable.size(), answer.get(answer.size() - 1), answer);

        _shortestPath = answer;
    }

    private List<Operation> getOperations(CSVTable firstTable, CSVTable secondTable, List<Point> points) {
        if (_operations == null) {
            List<Operation> operations = new ArrayList<>();
            Point prevPoint = points.get(0);
            for (Point nextPoint : points) {
                if (nextPoint.equals(prevPoint)) {
                    continue;
                }
                StringBuilder nextLine = new StringBuilder();
                if (prevPoint.x < nextPoint.x && prevPoint.y < nextPoint.y) {
                    for (int i = 0; i < firstTable.meta().size() - 1; i++) {
                        nextLine.append(firstTable.cell(prevPoint.x, i)).append(',');
                    }
                    nextLine.append(firstTable.cell(prevPoint.x, firstTable.meta().size() - 1));
                    operations.add(new Operation(OperationType.TAKE, nextLine.toString()));
                } else if (prevPoint.x < nextPoint.x) {
                    for (int i = 0; i < firstTable.meta().size() - 1; i++) {
                        nextLine.append(firstTable.cell(prevPoint.x, i)).append(',');
                    }
                    nextLine.append(firstTable.cell(prevPoint.x, firstTable.meta().size() - 1));
                    operations.add(new Operation(OperationType.DELETE, nextLine.toString()));
                } else {
                    for (int i = 0; i < firstTable.meta().size() - 1; i++) {
                        nextLine.append(secondTable.cell(prevPoint.y, i)).append(',');
                    }
                    nextLine.append(secondTable.cell(prevPoint.y, secondTable.meta().size() - 1));
                    operations.add(new Operation(OperationType.ADD, nextLine.toString()));
                }
                prevPoint = nextPoint;
            }
            _operations = operations;
        }
        return _operations;
    }

    public void editScript(CSVTable firstCSVTable, CSVTable secondCSVTable, PrintWriter out) {
        init(firstCSVTable, secondCSVTable);

        List<Operation> operations = getOperations(firstCSVTable, secondCSVTable, _shortestPath);

        int line = firstCSVTable.size();
        for (int i = operations.size() - 1; i >= 0; i--) {
            Operation operation = operations.get(i);
            switch (operation.type) {
                case TAKE -> line--;
                case DELETE -> {
                    out.println(Integer.toString(line) + 'd');
                    line--;
                }
                case ADD -> {
                    out.println(Integer.toString(line) + 'a');
                    out.println(operation.content);
                    out.println('.');
                }
                default -> throw new IllegalArgumentException("This operation is not supported.");
            }
        }
    }

    public void diff(CSVTable firstCSVTable, CSVTable secondCSVTable, PrintWriter out) {
        init(firstCSVTable, secondCSVTable);

        List<Operation> operations = getOperations(firstCSVTable, secondCSVTable, _shortestPath);
        operations.forEach(operation -> out.println(operation.type + " " + operation.content));
    }
}
