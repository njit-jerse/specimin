package com.example;

class Simple extends SomeOtherClass {
    void bar() {
        SomeOtherClass s = new SomeOtherClass(this);
    }

    public Simple() {
        super(null);
    }
}
