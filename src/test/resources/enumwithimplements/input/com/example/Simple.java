package com.example;

import com.example.Function;

public enum Simple implements Function<?, ?> {
    A, B;

    // target. Goal of this test is to make sure that Function, above, is created/preserved.
    void bar() {
        Simple s1 = Simple.A;
        Simple s2 = Simple.B;
    }
}
