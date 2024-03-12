package com.example;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

class Foo {

    @Positive
    public int baz() {
        throw new Error();
    }
}
