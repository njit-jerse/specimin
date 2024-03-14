package com.example;

import com.nameless.SomeClass;

public class Simple {

    public int testAnonymous() {
        int localVar = 42;
        SomeClass myObject = new SomeClass() {

            public int getLocalVar() {
                return localVar;
            }
        };
        return 0;
    }
}
