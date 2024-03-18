package com.example.util;

public class Cast {

    public static <T extends Object> T castNonNull(T t) {
        return t;
    }
}