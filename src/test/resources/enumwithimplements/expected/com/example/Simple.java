package com.example;

import com.example.Function;

public class Simple {
    private enum MyEnum implements Function<?, ?> {

        A, B;
    }

    void bar() {
        MyEnum a = MyEnum.A;
        MyEnum b = MyEnum.B;
    }
}
