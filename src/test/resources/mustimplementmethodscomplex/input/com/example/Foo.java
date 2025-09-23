package com.example;

// Note that the type variable map is String --> T --> E
public class Foo implements MyComparable<String> {
    @Override
    public int compareTo(String o) {
        throw new java.lang.Error();
    }

    // Target
    public void foo() {
        throw new java.lang.Error();
    }
}
