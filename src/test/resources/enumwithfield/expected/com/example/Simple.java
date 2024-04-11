package com.example;

public enum Simple {
    A, B;

    private static final String CHAR = null;

    void bar() {
        Simple.A;
        Simple.B;
        Simple.CHAR;
    }
}
