package com.example;

import java.util.List;

final class Simple {

    List<Foo> foos = null;

    // Target method. The goal of this test is to check that f is added to the local variable
    // scope, and not considered a field of the (non-existent) superclass.
    void bar() {
        for (Foo f : foos) {
            f.doSomething();
        }
    }
}
