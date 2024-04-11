package com.example;

import com.example.Function;

public enum Simple implements Function<?, ?> {
    A, B;

    void bar() {
        Simple s1 = Simple.A;
        Simple s2 = Simple.B;
    }
}
