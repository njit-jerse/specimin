package com.example;

import org.checkerframework.checker.signature.qual.ClassGetSimpleName;
import org.example.ToBeDeleted;

class Simple {

    public void baz() {
        @ToBeDeleted
        @ClassGetSimpleName
        String className = "";
    }
}
