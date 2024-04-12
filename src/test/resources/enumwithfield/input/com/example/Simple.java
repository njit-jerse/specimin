package com.example;

public enum Simple {
    A, B;

    private static final String CHAR = "char";

    // target. Goal of this test is to avoid crashing.
    void bar() {
        Simple s1 = Simple.A;
        Simple s2 = Simple.B;
        String s3 = Simple.CHAR;
    }
}
