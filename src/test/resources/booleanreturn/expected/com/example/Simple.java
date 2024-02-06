package com.example;

import org.example.ElementUtils;
import org.example.Foo;

class Simple {
    void test() {
        if (ElementUtils.isEffectivelyFinal()) {
        }
    }

    void testFoo(Foo f) {
        if (f.isGood()) {
        }
    }

    void testFoo2(Foo f) {
        if (f.good) {
        }
    }
}
