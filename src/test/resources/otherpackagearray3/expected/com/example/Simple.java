package com.example;

import org.example.anotherpkg.Type;

class Simple {
    Simple bar(org.example.Method method) {
        return new Simple(method.getTypes());
    }

    private Simple(Type[] types) {
        throw new java.lang.Error();
    }
}
