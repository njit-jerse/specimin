package com.example;

import org.checkerframework.checker.index.qual.NonNegative;

class Simple {
    @NonNegative
    public int test() {
        @NonNegative int postive = Foo.baz();
        return postive;
    }
}
