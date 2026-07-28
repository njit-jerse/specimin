package com.example;

import com.mathematic.Calculator;

class Simple {

    void bar() {
        var myCal = new Calculator();
        baz(myCal);
    }

    Object baz(Calculator obj) {
        throw new java.lang.Error();
    }
}
