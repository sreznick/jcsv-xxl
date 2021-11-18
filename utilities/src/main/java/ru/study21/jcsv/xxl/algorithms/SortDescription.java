package ru.study21.jcsv.xxl.algorithms;

import java.util.List;

public record SortDescription(List<KeyElement> keys) {
    public enum KeyType {
        STRING, LONG, BIG_INTEGER
    }

    public enum Order {
        ASCENDING, DESCENDING
    }

    public static record KeyElement(int field, KeyType keyType, Order order) {
        public static KeyElement asString(int field) {
            return new KeyElement(field, KeyType.STRING, Order.ASCENDING);
        }

        public static KeyElement asLong(int field) {
            return new KeyElement(field, KeyType.LONG, Order.ASCENDING);
        }
    }

    public static KeyElement asString(int field) {
        return new KeyElement(field, KeyType.STRING, Order.ASCENDING);
    }

    public static KeyElement asLong(int field) {
        return new KeyElement(field, KeyType.LONG, Order.ASCENDING);
    }

    public static SortDescription of(List<KeyElement> keys) {
        return new SortDescription(keys);
    }

    public static SortDescription of(KeyElement key) {
        return of(List.of(key));
    }

    public static SortDescription of(int field, KeyType key) {
        return of(List.of(new KeyElement(field, key, Order.ASCENDING)));
    }
}
