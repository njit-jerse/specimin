package com.example;

import org.example.Type;
import org.example.PrimitiveType;
import org.example.ClassOrInterfaceType;

class Simple {
    // Target method, based on an example from Randoop. This example differs
    // from twotypecorrect in that these instanceofs use Java 14 pattern variables.
    void bar(Type t) {
        if (t instanceof PrimitiveType p) {
            // do something with p...
        } else if (t instanceof ClassOrInterfaceType c) {
            // do something with c...
        }
    }
}
