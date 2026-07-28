package com.example;

import org.example.CustomException;
import org.example.Foo;

class Simple {
    void bar() {
        try {
            Foo.stuff();
        } catch (CustomException custom) {
        }
    }
}
