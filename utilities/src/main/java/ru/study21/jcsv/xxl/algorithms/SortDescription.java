package ru.study21.jcsv.xxl.algorithms;

import java.math.BigInteger;
import java.util.Comparator;
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

    public Comparator<List<String>> toRowComparator() {
        return (r1, r2) -> {
            for (KeyElement key : keys) {
                int cmpResult;
                switch (key.keyType()) {
                    case LONG -> {
                        cmpResult = Long.compare(
                                Long.parseLong(r1.get(key.field)),
                                Long.parseLong(r2.get(key.field))
                        );
                    }
                    case STRING -> {
                        cmpResult = r1.get(key.field).compareTo(r2.get(key.field));
                    }
                    case BIG_INTEGER -> {
                        cmpResult = new BigInteger(r1.get(key.field))
                                .compareTo(new BigInteger(r2.get(key.field)));
                    }
                    default -> throw new IllegalStateException("Internal error");
                }
                if(key.order == Order.DESCENDING) {
                    cmpResult = -cmpResult;
                }
                if (cmpResult != 0) {
                    return cmpResult;
                }
            }
            return 0;
        };
    }
}
