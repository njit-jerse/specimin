package com.example;

import javalanguage.Method;
class Simple {
    // Target method.
    void bar() {
        String x = "";
        Method method = new Method();
        try {
            method.solve();
        }
        catch (UnsupportedOperationException e) {
            x = e.toString();
        }
    }
}
