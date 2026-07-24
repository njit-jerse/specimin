package com.example;

public sealed class Foo permits Bar, Baz {
    void foo() {
        Qux qux;
    }
}

class Qux extends Baz { }