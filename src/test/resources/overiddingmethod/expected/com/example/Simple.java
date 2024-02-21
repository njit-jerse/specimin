package com.example;

import org.testing.UnsolvedType;

public class Simple extends B {

    public void bar(int x, UnsolvedType y) {
        throw new RuntimeException();
    }
}
