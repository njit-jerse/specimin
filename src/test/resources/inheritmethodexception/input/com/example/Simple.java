package com.example;

public class Simple extends Parent {
    @Override
    public void foo() throws UnknownException {
        throw new Error();
    }
}