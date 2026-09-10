package com.example;

import com.other.a.Foo;

public class Impl implements Rule {
    public String apply(com.other.b.Foo foo) {
        return "other";
    }

    public String apply(Foo foo) {
        return "real";
    }
}
