package com.example;

import org.example.Bar;
import org.example.Baz;
import org.example.Foo;
import org.example.Qux;

public class Simple {
    public void target(Bar b) {
        Baz<? super Foo> y = b.get2();
        y = new Baz<Qux>();
    }
}
