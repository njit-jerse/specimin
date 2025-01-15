package com.example;

public interface Baz<T> {

    default void doSomething(T value) {
        throw new java.lang.Error();
    }

    default void doSomething(int x) {
        throw new java.lang.Error();
    }
}
