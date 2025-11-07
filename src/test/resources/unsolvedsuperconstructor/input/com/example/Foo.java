package com.example;

class Foo extends Bar {
    // Target
    public Foo() {
       super("any argument");
    }

    // Also target
    public static Bar bar() {
        return new Bar("a consistent argument");
    }
}