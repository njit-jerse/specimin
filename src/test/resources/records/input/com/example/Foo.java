package com.example;

public class Foo {
    void foo() {
        MyRecord record1 = new MyRecord();

        record1.str();
        record1.foo();
        record1.x();

        MyRecord record2 = new MyRecord("", this);
        MyOtherRecord other = new MyOtherRecord("");
    }
}