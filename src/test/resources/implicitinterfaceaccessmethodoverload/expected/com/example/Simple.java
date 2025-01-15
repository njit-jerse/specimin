package com.example;

import org.testing.B;

abstract class Simple implements B {

    void target(boolean b) {
        if (b) {
            foo(1);
        } else {
            foo(1, "random string");
        }
    }

    int foo(int x) {
        throw new java.lang.Error();
    }
}
