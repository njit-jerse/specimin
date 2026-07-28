package com.example;

class Simple extends SomeOtherClass {
    // Target method.
    void bar() {
        SomeOtherClass s = new SomeOtherClass(0);
    }

    // This needs to be preserved, because otherwise the class won't compile.
    public Simple() {
        super(5);
    }
}
