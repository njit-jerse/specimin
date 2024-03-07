package com.example;

public class Simple<T> {

    T field;

    void foo(T input) {
        throw new Error();
    }

    void bar() {
        foo(field);
    }
}
