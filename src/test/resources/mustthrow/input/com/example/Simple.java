package com.example;

import org.example.Foo;
import org.example.CustomException;

class Simple {
    // Target method.
    void bar() {
        try {
            Foo.stuff();
        } catch (CustomException custom) {
            // do nothing
        }
    }
}
