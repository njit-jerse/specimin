package com.example;
import org.testing.UnsolvedType;

class Foo {
    protected Baz<UnsolvedType> field;
    void bar() {
        field.baz();
    }
}
