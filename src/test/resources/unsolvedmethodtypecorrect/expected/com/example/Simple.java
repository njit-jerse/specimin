package com.example;

import org.example.Foo;
import org.example.LocalVariables;

class Simple {
    void bar() {
        Foo f = new Foo();
        f.getLocals().set(0);
        final LocalVariables locals = f.getLocals();
    }
}
