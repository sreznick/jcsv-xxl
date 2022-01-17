package ru.study21.jcsv.xxl.common;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Utility {

    public static <T> List<T> slice(List<T> list, List<Integer> indices) {
        return indices.stream().map(list::get).collect(Collectors.toList());
    }

}
