package com.example;

class Simple {

    // Both used, both should be preserved.
    Object obj1, obj2;

    // Only first used.
    Object obj3, obj4;

    // Only second used.
    Object obj5, obj6;

    // Neither used.
    Object obj7, obj8;

    // Target method.
    void bar() {
        baz(obj1);
        baz(obj2);
        baz(obj3);
        baz(obj6);
    }

    Object baz(Object obj) {
        return obj.toString();
    }
}
