package com.example;

public class Simple {
    static void m() {}
    void foo(boolean b) {
        Fn c = b ? Simple::m : null;
    }
}
