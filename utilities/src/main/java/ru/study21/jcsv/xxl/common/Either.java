package ru.study21.jcsv.xxl.common;

public class Either<T, U> {
    private final T _left;
    private final U _right;
    private final boolean _isLeft;

    private Either(T left, U right, boolean isLeft) {
        _left = left;
        _right = right;
        _isLeft = isLeft;
    }

    public static <T, U> Either<T, U> left(T value) {
        return new Either<>(value, null, true);
    }

    public static <T, U> Either<T, U> right(U value) {
        return new Either<>(null, value, false);
    }

    public boolean isLeft() {
        return _isLeft;
    }

    public boolean isRight() {
        return !_isLeft;
    }

    public T left() {
        if (isLeft()) {
            return _left;
        }
        throw new IllegalStateException("it is not left");
    }

    public U right() {
        if (isRight()) {
            return _right;
        }
        throw new IllegalStateException("it is not right");
    }
}
