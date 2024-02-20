package com.example;

class Simple {

    Object obj1, obj2;

    void bar() {
        obj1 = baz(obj2);
    }

    Object baz(Object obj) {
        throw new Error();
    }
}
