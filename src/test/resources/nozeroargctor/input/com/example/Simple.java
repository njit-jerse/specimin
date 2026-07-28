package com.example;

class Simple extends SomeOtherClass {
    // Target method.
    void bar() {

    }

    // This needs to be preserved, because otherwise the class won't compile.
    public Simple() {
        super(null);
    }
}
