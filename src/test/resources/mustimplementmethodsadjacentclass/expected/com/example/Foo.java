package com.example;

public class Foo implements Baz {
    public void mustImplement() {
        throw new java.lang.Error();
    }

    void foo() {
        mustImplement();
        Bar bar = new Bar();
    }
}