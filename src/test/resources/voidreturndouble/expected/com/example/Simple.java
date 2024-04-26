package com.example;

import org.example.MethodGen;
import org.example.Foo;

public class Simple {

    public void bar(MethodGen m) {
        Foo foo = new MyFoo();
        foo.methodGen(m);
    }
}