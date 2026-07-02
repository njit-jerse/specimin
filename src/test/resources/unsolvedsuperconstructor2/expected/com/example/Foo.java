package com.example;

class Foo extends Bar {
    public Foo() {
       super("any argument", "some other argument");
    }

    public static Bar bar() {
        Foo foo = new Foo();
        return new Bar("a consistent argument");
    }
}