package com.example;

import org.example.Type;

class Simple {
    void bar(org.example.Method method) {
        baz(method.getTypes());
    }

    void baz(Type[] types) {
        throw new Error();
    }
}
