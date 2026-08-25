package com.example;

public class Simple {
    void foo(Anno anno) {
        sinkClass(anno.annotationType());
        sinkString(anno.toString());
    }

    void sinkClass(Class<?> clazz) {}

    void sinkString(String value) {}
}
