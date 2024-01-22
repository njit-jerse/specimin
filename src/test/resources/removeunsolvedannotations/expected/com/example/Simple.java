package com.example;

import org.checkerframework.checker.signature.qual.ClassGetSimpleName;

class Simple {

    public void baz() {
        @ClassGetSimpleName
        String className = "";
    }
}
