package com.example;

// Baz.java
public interface Baz<T> {
    void doSomething(T value);
    // this method will be removed.
    void doSomething();
    void doSomething(int x);

}


