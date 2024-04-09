package com.example;

public interface Foo extends Baz {
    default void printMethod() {
        System.out.println("Hello World");
    }
}
