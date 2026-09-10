package com.example;

import com.example.a.Foo;

public class Impl implements Rule {

    public String apply(Foo foo) {
        throw new java.lang.Error();
    }
}
