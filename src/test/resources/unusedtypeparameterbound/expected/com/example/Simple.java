package com.example;

class Simple<T, V> {

    void bar() {
        Object obj = new Object();
        obj = baz(obj);
    }

    Object baz(Object obj) {
        throw new Error();
    }
}