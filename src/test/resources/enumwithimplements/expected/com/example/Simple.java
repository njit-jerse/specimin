package com.example;

public class Simple {
    private enum MyEnum {

        A, B
    }

    void bar() {
        MyEnum a = MyEnum.A;
        MyEnum b = MyEnum.B;
    }
}
