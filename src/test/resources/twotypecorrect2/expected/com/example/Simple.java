package com.example;

import org.example.ClassOrInterfaceType;
import org.example.PrimitiveType;
import org.example.Type;

class Simple {
    void bar(Type t) {
        if (t instanceof PrimitiveType p) {
        } else if (t instanceof ClassOrInterfaceType c) {
        }
    }
}
