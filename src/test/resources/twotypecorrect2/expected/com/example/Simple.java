package com.example;

import org.example.Type;
import org.example.PrimitiveType;
import org.example.ClassOrInterfaceType;

class Simple {
    void bar(Type t) {
        if (t instanceof PrimitiveType p) {
        } else if (t instanceof ClassOrInterfaceType c) {
        }
    }
}
