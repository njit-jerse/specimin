package com.example;

import org.testing.UnsolvedType;

public interface Baz<T> {
    void doSomething(T value);
    // this method will be removed.
    UnsolvedType doSomething();
    void doSomething(int x);

}


