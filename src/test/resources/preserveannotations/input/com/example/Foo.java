package com.example;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

class Foo {
    @Positive
    public static int baz() {
        return 1;
    }

    @NonNegative
    public static int foo() { return 0;}
}
