package com.example;

import org.fortest.UnsolvedType;

class Foo {

    int bar() {
        try {
            int i = 0;
            throw new UnsolvedType("Exception");
        } catch (Exception e) {
            return 2;
        }
    }
}
