package com.example.util;

public class Cast {

    public static <T extends Object> T castToNonNull(T t) {
        throw new Error();
    }
}