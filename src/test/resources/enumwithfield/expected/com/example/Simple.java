package com.example;

public enum Simple {
    A, B;

    private static final String CHAR = "char";

    void bar() {
        Simple.A;
        Simple.B;
        Simple.CHAR;
    }
}
