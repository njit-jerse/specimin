package com.example;

import org.testing.Baz;

class Simple<T extends Baz, V> {

    void bar(T bazObject) {
        bazObject.bazMethod();
    }
}
