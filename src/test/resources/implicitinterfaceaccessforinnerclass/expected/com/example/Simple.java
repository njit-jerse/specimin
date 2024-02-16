package com.example;

import org.testing.B;
import org.fortesting.Parent2;

public class Simple extends B {

    void foo() {
        throw new Error();
    }

    class InnerClass2 extends Parent2 {

        void method2() {
            method1();
            foo();
        }
    }
}
