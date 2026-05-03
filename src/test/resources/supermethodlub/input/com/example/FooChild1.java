package com.example;

import org.example.Foo;

public class FooChild1 extends Foo {
    @Override
    public Integer getNum() {
        return 0;
    }

    @Override
    public Unsolved1 getUnsolved() {
        throw new java.lang.Error();
    }
}
