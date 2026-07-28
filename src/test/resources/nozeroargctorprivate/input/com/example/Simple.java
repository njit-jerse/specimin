package com.example;

class Simple extends SomeOtherClass {
    // Target method. Its body keeps the public constructor of SomeOtherClass in the slice.
    void bar() {
        new SomeOtherClass(1, 2);
    }

    // A constructor needs to be preserved, because otherwise the class won't compile:
    // SomeOtherClass keeps both of its constructors, so it has no default constructor.
    public Simple() {
        super(1, 2);
    }
}
