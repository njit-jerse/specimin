package com.example;

import org.example.Foo;

class Simple extends Foo {
    void bar() {
        // This is a superclass field. It can't be a synthetic
        // type, though, because then this assignment in the target
        // method won't typecheck.
        x = 10;
    }
}
