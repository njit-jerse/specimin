package com.example;

import org.testing.UnsolvedType;

class Simple<I extends UnsolvedType> {
    I field;
    void bar() {
        field.print();
    }
}
