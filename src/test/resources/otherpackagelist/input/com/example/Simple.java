package com.example;

import java.util.List;
import org.example.anotherpkg.Type;

class Simple {
    // Target method.
    Simple bar(org.example.Method method) {
        return new Simple(method.getTypes());
    }

    private Simple(List<? extends Type> types) {

    }
}
