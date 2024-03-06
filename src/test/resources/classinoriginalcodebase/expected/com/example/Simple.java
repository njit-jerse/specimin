package com.example;

import org.testing.UnsolvedType;

public class Simple {

    public void foo(SomeClass inner) {
        throw new RuntimeException();
    }
}

class SomeClass extends UnsolvedType {
}
