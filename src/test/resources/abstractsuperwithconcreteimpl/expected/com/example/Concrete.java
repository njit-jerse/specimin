package com.example;

public class Concrete implements Super {
    public Foo foo() {
        throw new java.lang.Error();
    }

    public Bar bar() {
        throw new java.lang.Error();
    }
}
