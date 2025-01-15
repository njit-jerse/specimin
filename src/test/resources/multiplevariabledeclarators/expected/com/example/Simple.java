package com.example;

class Simple {

    Object obj1, obj2;

    Object obj3;

    Object obj6;

    void bar() {
        baz(obj1);
        baz(obj2);
        baz(obj3);
        baz(obj6);
    }

    Object baz(Object obj) {
        throw new java.lang.Error();
    }
}
