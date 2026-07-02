package com.example;

class Foo extends Bar {
    public Foo() {
       super("any argument");
    }

    public static Bar bar() {
        return new Bar("a consistent argument");
    }
}