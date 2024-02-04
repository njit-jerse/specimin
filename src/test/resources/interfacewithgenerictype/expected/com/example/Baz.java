package com.example;

// Baz.java
public interface Baz<T> {

    void doSomething(T value);

    // sadly we can't remove this method.
    void doSomething(int x);
}
