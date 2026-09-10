package com.example;

import com.example.a.Foo;

public class Impl implements Rule {
    // Declared first, and its parameter type is not on the source path, so this is the
    // overload that matches only approximately.
    public String apply(com.other.b.Foo foo) {
        return "wrong";
    }

    // The real implementation of Rule#apply.
    public String apply(Foo foo) {
        return "right";
    }
}
