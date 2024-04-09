package com.example;

public interface Foo {
    default void printMethod() {
        System.out.println("Hello World");
    }
}
