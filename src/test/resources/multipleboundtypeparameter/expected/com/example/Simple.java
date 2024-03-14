package com.example;

import org.testing.UnsolvedType;
import org.testing.UnsolvedType2;
import org.testing.UnsolvedType3;

class Simple<I extends UnsolvedType & UnsolvedType2 & UnsolvedType3> {

    I field;

    void bar() {
        field.print();
    }
}
