package com.example;

public class Foo {
    public Foo(int x, Object f) {
        // will be preserved, because it's called in the target method.
        // that will mean that there is no default constructor in this class.
        // In this version of the test, there are additional statements
        // here, which ought to be removed:
        System.out.println("remove me!");
    }
}