package com.example;

import static org.example.Preconditions.checkArgument;
import static org.example.Preconditions.checkState;

public class Simple {
    // Target method.
    void bar() {
        checkArgument(null);
    }

    void baz(Object o) {
        checkState(o);
    }
}
