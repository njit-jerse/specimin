package com.example;

import org.example.MultipleMethods;

public class OverloadExamples {
    void test(MultipleMethods m) {
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