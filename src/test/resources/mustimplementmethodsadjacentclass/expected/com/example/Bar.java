package com.example;

public class Bar implements Baz {
    public void mustImplement() {
        throw new java.lang.Error();
    }
}