package com.example;

import java.io.LineNumberReader;

class Simple extends LineNumberReader {
    // Target method.
    void bar() {

    }

    // This needs to be preserved, because otherwise the class won't compile.
    public Simple() {
        // Note that this input won't compile (String <: Reader is obviously false), but
        // that's okay. This is just here to test that Specimin will replace any argument
        // with a null literal.
        super("bananas");
    }
}
