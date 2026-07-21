package com.example;

import org.example.Unsolved;

public class Simple {

    Unsolved field;

    void target() {
        throw new IllegalStateException("bad value: " + field);
    }
}
