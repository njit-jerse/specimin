package com.example;

import  org.checkerframework.checker.nullness.qual.Nullable;
import org.testing.UnsolvedType;

class Simple {
    void bar(byte @Nullable [] first, @Nullable UnsolvedType second) {
        throw new java.lang.Error();
    }

    void foo() {
        System.out.println("This method will be removed!");
    }
}

