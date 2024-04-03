package com.example;

public class Simple extends Foo {
    // Target method.
    public static void bar() {
        Simple s = new Simple(5);
        Foo f = new Foo(5, null);
    }

    public Simple(int x) {
        // must be preserved, because otherwise the output
        // will not be compilable (Foo has no default ctor)
        super(x, null);
        // In this version of the test, the following additional
        // statement is present, which ought to be removed:
        toBeRemoved();
    }

    public static void toBeRemoved() {

    }
}
