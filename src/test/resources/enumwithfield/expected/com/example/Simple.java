package com.example;

public enum Simple {
    A, B;

    private static final String CHAR = null;

    void bar() {
        Simple s1 = Simple.A;
        Simple s2 = Simple.B;
        String s3 = Simple.CHAR;
    }
}
