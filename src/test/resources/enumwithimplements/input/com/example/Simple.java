package com.example;

import com.example.Function;

public class Simple {
    private enum MyEnum implements Function<MyEnum, MyEnum> {
        A, B;

        public void foo() {

        }
    }

    // target.
    void bar() {
        MyEnum a = MyEnum.A;
        MyEnum b = MyEnum.B;
    }
}
