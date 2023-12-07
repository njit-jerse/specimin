package com.example;
import com.mathematic.Calculator;

class Simple {
    // Target method.
    void bar() {
        var myCal = new Calculator();
        baz(myCal);
    }

    Object baz(Calculator obj) {
        throw new RuntimeException();
    }
}
