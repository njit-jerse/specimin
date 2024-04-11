package com.example;

public enum Simple {
    A, B;

    private static final String CHAR = "char";

    // target. Goal of this test is to avoid crashing.
    void bar() {
        Simple.A;
        Simple.B;
        Simple.CHAR;
    }
}
