package com.example;

import org.fortesting.Parent2;

public class Simple {

    void foo() {
        throw new java.lang.Error();
    }

    class InnerClass2 extends Parent2 {

        void method2() {
            method1();
            foo();
        }
    }
}
