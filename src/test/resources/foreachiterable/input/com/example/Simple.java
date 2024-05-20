package com.example;

import org.example.Baz;
import org.example.Foo;

class Simple {
    // Target method.
    void bar(Baz b) {
        for (Foo f : b.getFoos()) {
            // do something with f
        }
    }
}
