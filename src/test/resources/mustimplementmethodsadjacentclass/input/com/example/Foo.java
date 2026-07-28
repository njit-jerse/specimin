package com.example;

public class Foo implements Baz {
    public void mustImplement() { }

    void foo() {
        mustImplement();
        Bar bar = new Bar();
    }
}