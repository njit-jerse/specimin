package com.example;

public class Simple {
    Simple() {
        this(1, 2);
    }

    Simple(int a, int b) {
        throw new RuntimeException();
    }
}
