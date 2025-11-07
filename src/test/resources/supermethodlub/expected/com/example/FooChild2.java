package com.example;

import org.example.Foo;

public class FooChild2 extends Foo {
    public Long getNum() {
        throw new java.lang.Error();
    }

    public Unsolved2 getUnsolved() {
        throw new java.lang.Error();
    }
}
