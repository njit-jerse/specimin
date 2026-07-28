package com.example;

public sealed class Foo permits Bar, Baz {
    void foo() {
        Qux qux;
    }
}

// Since Qux extends Baz, Baz cannot be final
class Qux extends Baz { }