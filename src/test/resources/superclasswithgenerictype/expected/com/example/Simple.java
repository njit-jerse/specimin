package com.example;

import org.testing.Parent;

class Simple<T> extends Parent<T> {

    public void bar() {
        throw new java.lang.Error();
    }
}
