package com.example;

import java.util.List;
import org.example.anotherpkg.Type;

class Simple {
    Simple bar(org.example.Method method) {
        return new Simple(method.getTypes());
    }

    private Simple(List<? extends Type> types) {
        throw new Error();
    }
}
