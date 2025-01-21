package com.example;

public interface Baz<E> extends org.testing.Baz<E> {

    default void bar() {
        throw new java.lang.Error();
    }
}
