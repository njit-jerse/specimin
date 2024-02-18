package com.example;

import org.testing.B;
import org.testing.C;
import org.testing.D;

abstract class Simple implements B, C, D {
    int foo() {
        return baz();
    }
}
