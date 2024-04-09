package com.example;

public interface Baz<E> {
    public default boolean containsAll(com.example.Baz<?> parameter0) {
        throw new Error();
    }
}
