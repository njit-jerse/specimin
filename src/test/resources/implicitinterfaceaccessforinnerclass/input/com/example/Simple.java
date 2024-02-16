package com.example;

import org.fortesting.Parent2;

public class Simple {

    void foo() {
        throw new RuntimeException();
    }

    class InnerClass1 {
        void method1() {
            throw new RuntimeException();
        }
    }

    class InnerClass2 extends Parent2 {
        void method2() {
            // method1() is a method from Parent2 rather than InnerClass1.
            method1();
            // foo() is a method from Simple rather than Parent2.
            foo();
        }
    }
}
