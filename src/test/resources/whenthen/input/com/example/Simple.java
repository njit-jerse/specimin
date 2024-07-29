package com.example;

import static org.example.TestUtil.when;
import static org.example.TestUtil.mock;

class Simple {
    // Target method.
    void bar() {
        when(5).then("Foo");
        when("bar").then(mock(1));
    }
}
