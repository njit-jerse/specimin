package com.example;

import org.testing.Baz;
import org.testing.Foo;

class Simple<T extends Baz, V extends Foo> {
    void bar() {
        Object obj = new Object();
        obj = baz(obj);
    }
    Object baz(Object obj) {
        return obj.toString();
    }
}
