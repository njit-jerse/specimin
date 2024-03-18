package com.example.util;

public class Cast {

    public static <T extends Object> T castNonNull(T t) {
        throw new Error();
    }
}