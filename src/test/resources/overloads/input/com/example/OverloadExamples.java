package com.example;

import org.example.MultipleMethods;

public class OverloadExamples {
    void test(MultipleMethods m) {
        // I expect to create 1 method with no arguments,
        // two methods with one argument, and four methods
        // with two arguments - that is, the powerset of String
        // and int. TODO: more examples for return types.
        m.example();
        m.example("foo");
        m.example(5);
        m.example(null);
        m.example("foo", 5);
        m.example(5, "foo");
        m.example(5, 5);
        m.example("foo", "foo");
        m.example(null, null);
    }
}