package com.example;

import org.testing.UnsolvedInterface;

public abstract class Simple implements UnsolvedInterface {
    // Target method.
    int bar() {
        return this.foo();
    }
}
