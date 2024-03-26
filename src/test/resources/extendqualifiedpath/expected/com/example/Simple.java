package com.example;

class Simple<T> implements Baz<T> {

    void bar() {
        Object obj = new Object();
        obj = baz(obj);
    }
}
