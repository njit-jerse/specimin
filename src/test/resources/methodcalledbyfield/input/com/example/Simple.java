package com.example;

import org.math.Calculator;

class Simple {
    static final Calculator theField = new Calculator();
    void test() {
        int x = theField.doMultiplication(1, 2);
    }
}
