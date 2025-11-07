package com.example;

import org.example.Baz;

public class SomeChild extends SomeParent {
    @Override
    public Baz method() {
        throw new java.lang.Error();
    }
}
