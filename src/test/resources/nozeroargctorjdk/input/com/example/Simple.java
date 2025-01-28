package com.example;

import java.io.LineNumberReader;

class Simple extends LineNumberReader {
    // Target method.
    void bar() {

    }

    // This needs to be preserved, because otherwise the class won't compile.
    public Simple() {
        super(null);
    }
}
