package com.example;

public class Simple {
    void foo(boolean b) {
        Fn f = b ? () -> {} : null;
    }
}
