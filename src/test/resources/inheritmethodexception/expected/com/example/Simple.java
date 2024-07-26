package com.example;

public class Simple extends Parent {

    public void foo() throws UnknownException {
        throw new Error();
    }
}
