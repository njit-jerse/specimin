package com.example;

import org.testing.MyException;

class Simple {

    void bar(String input) {
        if (input.isBlank()) {
            throw new MyException("A blank String is no good");
        }
    }
}
