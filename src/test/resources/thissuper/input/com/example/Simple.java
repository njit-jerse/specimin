package com.example;

import org.example.Foo;

public class Simple extends Foo {
    // Target method.
    void bar() {
        // This field must have been declared in the super class Foo
        // (or one of its superclasses).
        this.y = null;
    }
}
