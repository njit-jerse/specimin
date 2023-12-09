package com.example;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

class Simple {

    @NonNegative
    public int test() {
        @Positive
        int postive = 10;
        throw new RuntimeException();
    }
}
