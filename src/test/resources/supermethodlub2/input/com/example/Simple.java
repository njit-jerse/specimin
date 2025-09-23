package com.example;

import org.example.Foo;
import org.example.Bar;

public class Simple {
    public void foo() {
        SomeChild child = new SomeChild();
        foo(child.method());
    }

    private void foo(Foo foo) {

    }

    private void foo(Bar bar) {

    }
}
