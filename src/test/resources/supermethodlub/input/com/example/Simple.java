package com.example;

public class Simple {
    public void foo() {
        FooChild1 foo1 = new FooChild1();
        foo1.getNum();
        foo1.getUnsolved();

        FooChild2 foo2 = new FooChild2();
        foo2.getNum();
        foo2.getUnsolved();
    }
}
