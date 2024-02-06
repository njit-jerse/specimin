package com.example;

import org.example.ElementUtils;
import org.example.Foo;

class Simple {
    // First target method: static call
    void test() {
        if (ElementUtils.isEffectivelyFinal()) {
            // do something
        }
    }

    // Second target method: instance method
    void testFoo(Foo f) {
        if (f.isGood()) {
            // do something
        }
    }

    // Second target method: unsolved field
    void testFoo2(Foo f) {
        if (f.good) {
            // do something
        }
    }
}
