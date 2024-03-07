package com.example;

public class Simple<T> {

    T field;
    void foo(T input) {
        System.out.println("This method will be emptied!");
    }

    void bar() {
        foo(field);
    }
}