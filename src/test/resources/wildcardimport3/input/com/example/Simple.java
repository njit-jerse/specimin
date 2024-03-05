package com.example;

// The input has Foo in the second of these packages,
// so we should put it there.
import org.example.*;
import org.anotherexample.*;

class Simple {
    // Target method.
    void bar() {
        Foo obj = new Foo();
        obj.fooMethod();
    }
}
