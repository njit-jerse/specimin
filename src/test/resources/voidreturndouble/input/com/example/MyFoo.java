package com.example;

import org.example.Foo;
import org.example.MethodGen;

public class MyFoo extends Foo {

    public MyFoo() {
        throw new java.lang.Error();
    }

    @Override
    public void methodGen(MethodGen m) { }
}