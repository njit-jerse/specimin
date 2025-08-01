package com.example;

import org.example.LocalVariables;
import org.example.Foo;

class Simple {
    void bar() {
        Foo f = new Foo();
        f.getLocals().set(0);
        baz();
        final LocalVariables locals = f.getLocals();
    }
}
