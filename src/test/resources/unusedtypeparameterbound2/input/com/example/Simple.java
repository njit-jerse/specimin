package com.example;

import org.testing.Baz;
import org.testing.Foo;

class Simple<T extends Baz, V extends Foo> {
    void bar(T bazObject) {
        bazObject.bazMethod();
    }
    Object baz(Object obj) {
        return obj.toString();
    }
}
