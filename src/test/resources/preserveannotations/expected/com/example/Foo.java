package com.example;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

class Foo {

    @Positive
    public static int baz() {
        throw new java.lang.Error();
    }
}
