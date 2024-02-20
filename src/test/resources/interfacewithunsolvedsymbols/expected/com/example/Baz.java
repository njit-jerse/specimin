package com.example;

public interface Baz<T> {

    default void doSomething(T value) {
        throw new Error();
    }

    default void doSomething(int x) {
        throw new Error();
    }
}
