package com.example;

import org.testing.UnsolvedType;

public interface Baz<T> {

    default UnsolvedType doSomething(T value) {
        throw new Error();
    }
}
