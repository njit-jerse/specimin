package com.example;

import org.example.Foo;

public class FooChild2 extends Foo {
    @Override
    public Long getNum() {
        return 0L;
    }

    @Override
    public Unsolved2 getUnsolved() {
        throw new java.lang.Error();
    }
}
