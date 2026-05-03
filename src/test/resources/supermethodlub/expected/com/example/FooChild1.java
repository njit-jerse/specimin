package com.example;

import org.example.Foo;

public class FooChild1 extends Foo {
    public Integer getNum() {
        throw new java.lang.Error();
    }
    
    public Unsolved1 getUnsolved() {
        throw new java.lang.Error();
    }
}
