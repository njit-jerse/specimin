package com.example;

// These wildcard imports shouldn't prevent us from finding the Foo
// in this package.
import org.example.*;
import org.anotherexample.*;

class Simple {
    // Target method.
    void bar() {
        Foo obj = new Foo();
        obj.fooMethod();
    }
}
