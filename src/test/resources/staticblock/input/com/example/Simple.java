package com.example;
import org.testing.Baz;

class Simple {

    static int field;
    void test() {
    }

    static {
        Baz obj = new Baz();
        field = obj.process();
    }
}
