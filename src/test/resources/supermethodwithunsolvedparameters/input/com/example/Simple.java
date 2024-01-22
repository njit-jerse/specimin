package com.example;

import org.simple.SuperSimple;
import org.example.Unsolved;

class Simple extends SuperSimple {
    void bar() {
        super.foo(baz());
    }

    public Unsolved baz() {
        throw new Error();
    }
}
