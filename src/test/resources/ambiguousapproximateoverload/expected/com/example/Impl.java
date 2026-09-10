package com.example;

import com.other.a.Foo;

public class Impl implements Rule {

    public String apply(com.other.b.Foo foo) {
        throw new java.lang.Error();
    }

    public String apply(Foo foo) {
        throw new java.lang.Error();
    }
}
