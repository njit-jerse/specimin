package com.example;

public class Simple {
    private Super s;
    public void foo() {
        s.foo();
        Concrete concrete = new Concrete();
        s.bar();
    }
}
