package com.example;

import com.nameless.SomeClass;

public class Simple {

    public int testAnonymous() {
        int localVar = 42;
        SomeClass myObject = new SomeClass() {

            @Override
            public int getLocalVar() {
                return localVar;
            }
        };
        return 0;
    }
}
