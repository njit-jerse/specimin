package com.example;

import org.example.Unsolved;
import org.simple.SuperSimple;

class Simple extends SuperSimple {

    void bar() {
        super.foo(baz());
    }

    public Unsolved baz() {
        throw new java.lang.Error();
    }
}
