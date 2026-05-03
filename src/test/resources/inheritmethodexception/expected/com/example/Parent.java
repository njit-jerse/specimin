package com.example;

public class Parent {

    public void foo() throws com.example.UnknownException {
        throw new java.lang.Error();
    }
}
