package com.example;

public class Simple {
    static String make() { return null; }
    void foo() {
        ObjectConstructor<String> c = Simple::make;
    }
}
