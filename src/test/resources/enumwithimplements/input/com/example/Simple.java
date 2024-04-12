package com.example;

import com.example.Function;

public class Simple {
    private enum MyEnum implements Function<?, ?> {
        A, B;
    }

    // target. Goal of this test is to make sure that Function, above, is created/preserved.
    void bar() {
        MyEnum a = MyEnum.A;
        MyEnum b = MyEnum.B;
    }
}
