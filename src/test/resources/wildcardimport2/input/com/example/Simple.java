package com.example;

// No way to tell which of these contains Foo, so
// we default to the first one.
import org.example.*;
import org.anotherexample.*;

class Simple {
    // Target method.
    void bar() {
        Foo obj = new Foo();
        obj.fooMethod();
    }
}
