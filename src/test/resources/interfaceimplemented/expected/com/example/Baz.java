package com.example;

public interface Baz {

    default void doSomething() {
        throw new java.lang.Error();
    }
}
