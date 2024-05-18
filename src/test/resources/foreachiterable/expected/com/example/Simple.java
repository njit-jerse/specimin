package com.example;

import org.example.Baz;
import org.example.Foo;

class Simple {
    void bar(Baz b) {
        for (Foo f : b.getFoos()) {
        }
    }
}
