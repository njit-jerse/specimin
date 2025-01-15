package com.example;

import org.example.Foo;
import org.example.CustomException;

class Simple {
    void bar() {
        try {
            Foo.stuff();
        } catch (CustomException custom) {
        }
    }
}
