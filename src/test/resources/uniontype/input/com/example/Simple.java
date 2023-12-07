package com.example;

import com.github.javaparser.resolution.UnsolvedSymbolException;
import javalanguage.Method;

import java.io.IOException;

class Simple {
    // Target method.
    void bar() {
        String x = "";
        Method method = new Method();
        try {
            method.solve();
            // this line is to avoid the compilation error when an exception is never thrown
            throw new UnsolvedSymbolException();
        }
        catch (UnsupportedOperationException | UnsolvedSymbolException e) {
            x = e.toString();
        }
    }
}
