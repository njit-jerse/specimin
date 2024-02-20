package com.example;

class Simple {
    // Target method.
    void bar() {
        Object obj = new Object();
        obj = baz(obj);
    }

    Object baz(Object obj) {
        return obj.toString();
    }
}
