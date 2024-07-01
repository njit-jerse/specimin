package com.example;

import org.example.anotherpkg.Type;

class Simple {
    // Target method.
    Simple bar(org.example.Method method) {
        return new Simple(method.getTypes());
    }

    private Simple(Type[] types) {

    }
}
