package ru.study21.jcsv.xxl.algorithms;

import ru.study21.jcsv.xxl.algorithms.SortDescription.KeyType;
import ru.study21.jcsv.xxl.data.CSVTable;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
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

    private record Operation(OperationType type, String content) {
    }

    private void addPointsBetweenTwoPointsToList(int x, int y, Point prevPoint, List<Point> result) {
        int xPrev = prevPoint.x, yPrev = prevPoint.y;

        while (x != xPrev || y != yPrev) {
            if (x < xPrev && y < yPrev) {
                xPrev--;
                yPrev--;
            } else if (x < xPrev) {
                xPrev--;
            } else {
                yPrev--;
            }
            result.add(new Point(xPrev, yPrev));
        }
    }

    private List<Point> getShortestPath(List<int[]> steps, int d, int k, int length) {
        int x = steps.get(d)[(k + length) % length];
        int y = x - k;

        List<Point> path = new ArrayList<>();
        path.add(new Point(x, y));

        for (; d > 0; d--) {
            if (k == -d) {
                k++;
            } else if (k == d) {
                k--;
            } else {
                if (steps.get(d - 1)[(k - 1 + length) % length] < steps.get(d - 1)[(k + 1 + length) % length]) {
                    k++;
                } else {
                    k--;
                }
            }

            x = steps.get(d)[(k + length) % length];
            y = x - k;

            addPointsBetweenTwoPointsToList(x, y, path.get(path.size() - 1), path);
        }
        addPointsBetweenTwoPointsToList(0, 0, path.get(path.size() - 1), path);

        Collections.reverse(path);

        return path;
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

    private void init(CSVTable firstCSVTable, CSVTable secondCSVTable) {
        int firstCSVTableSize = firstCSVTable.size();
        int secondCSVTableSize = secondCSVTable.size();

        int[] nextStep = new int[2 * (firstCSVTableSize + secondCSVTableSize) + 1];
        nextStep[1] = 0;

        List<int[]> steps = new ArrayList<>();

        int d, k = 0;
        for (d = 0; d <= firstCSVTableSize + secondCSVTableSize; d++) {
            boolean end = false;

            for (k = -d; k <= d; k += 2) {
                boolean down;

                int firstIndexInArray = (k - 1 + nextStep.length) % nextStep.length;
                int secondIndexInArray = (k + 1 + nextStep.length) % nextStep.length;

                if (k == -d) {
                    down = true;
                } else if (k == d) {
                    down = false;
                } else {
                    down = nextStep[firstIndexInArray] < nextStep[secondIndexInArray];
                }

                int kPrev = down ? secondIndexInArray : firstIndexInArray;

                int xEnd = down ? nextStep[kPrev] : nextStep[kPrev] + 1;
                int yEnd = xEnd - k;

                while (xEnd < firstCSVTableSize && yEnd < secondCSVTableSize &&
                        equalsLine(firstCSVTable, secondCSVTable, xEnd, yEnd, _keys)) {
                    xEnd++;
                    yEnd++;
                }

                nextStep[(k + nextStep.length) % nextStep.length] = xEnd;

                if (xEnd >= firstCSVTableSize && yEnd >= secondCSVTableSize) {
                    end = true;
                    break;
                }
            }
            if (end) {
                steps.add(nextStep);
                break;
            }
            steps.add(nextStep.clone());
        }

        _shortestPath = getShortestPath(steps, d, k, nextStep.length);
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
