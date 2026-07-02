package com.example;

import org.example.Foo;
import org.example.MethodGen;

public class Simple {

    public void bar(MethodGen m) {
        Foo foo = new MyFoo();
        foo.methodGen(m);
    }
}