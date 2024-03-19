package com.example.nullness;

public class Nullness {

    public static <T extends Object> T castNonNull(T t) {
        throw new Error();
    }
}