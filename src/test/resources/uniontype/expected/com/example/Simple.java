package com.example;

import com.github.javaparser.resolution.UnsolvedSymbolException;
import javalanguage.Method;

class Simple {

    void bar() {
        String x = "";
        Method method = new Method();
        try {
            method.solve();
            throw new UnsolvedSymbolException();
        } catch (UnsupportedOperationException | UnsolvedSymbolException e) {
            x = e.toString();
        }
    }
}
