package com.example;

class SomeOtherClass {
    // This constructor has the fewest parameters, but Specimin must not use it as the
    // target of a generated super(...) call, because a subclass cannot call it.
    private SomeOtherClass(Object object) {

    }

    public SomeOtherClass(int first, int second) {

    }

    // Target method. Its body keeps the private constructor above in the slice.
    static SomeOtherClass make() {
        return new SomeOtherClass(null);
    }
}
