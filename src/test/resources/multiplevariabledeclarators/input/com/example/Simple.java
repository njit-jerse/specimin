package com.example;

class Simple {

    Object obj1, obj2;

    // Target method.
    void bar() {
        obj1 = baz(obj2);
    }

    Object baz(Object obj) {
        return obj.toString();
    }
}
