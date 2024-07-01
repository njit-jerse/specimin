package com.example;

import org.example.Type;

class Simple {
    Simple bar(org.example.Method method) {
        return new Simple(method.getTypes());
    }

    private Simple(Type[] types) {
        throw new Error();
    }
}
