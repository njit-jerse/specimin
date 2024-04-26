package com.example;

import org.example.LocalVariables;

class Simple {
    // Target method.
    void bar() {
        Foo f = new Foo();
        f.getLocals().set(0);
    }

    void baz(Foo f) {
        // To trigger JavaTypeCorrect.
        LocalVariables locals = f.getLocals();
    }
}
