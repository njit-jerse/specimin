package com.example;

class Simple extends SomeOtherClass {
    void bar() {
        new SomeOtherClass(1, 2);
    }

    public Simple() {
        super(0, 0);
    }
}
