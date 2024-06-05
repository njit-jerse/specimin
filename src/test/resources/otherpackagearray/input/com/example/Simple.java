package com.example;

class Simple {
    // Target method.
    void bar(org.example.Method method) {
        baz(method.getTypes());
    }

    void baz(Type[] types) {

    }
}
