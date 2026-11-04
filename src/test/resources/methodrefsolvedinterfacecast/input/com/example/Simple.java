package com.example;

public class Simple {
    static void m() {}
    void foo(boolean b) {
        Object c = (Fn) Simple::m;
    }
}
