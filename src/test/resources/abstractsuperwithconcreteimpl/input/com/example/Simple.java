package com.example;

public class Simple {
    private Super s;
    public void foo() {
        // Both foo and bar should be preserved in Super and Concrete
        s.foo();
        Concrete concrete = new Concrete();
        s.bar();
    }
}
