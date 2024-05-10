package com.example;

import org.example.Type;
import org.example.PrimitiveType;
import org.example.ClassOrInterfaceType;

class Simple {
    // Target method, based on an example from Randoop.
    void bar(Type t) {
        if (t instanceof PrimitiveType || t instanceof ClassOrInterfaceType) {
            // do something...
        }
    }
}
