package com.example;

class SomeOtherClass {
    private SomeOtherClass(Object object) {
        throw new java.lang.Error();
    }

    public SomeOtherClass(int first, int second) {
        throw new java.lang.Error();
    }

    static SomeOtherClass make() {
        return new SomeOtherClass(null);
    }
}
