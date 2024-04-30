package org.example;

public class Outer {

    public static class Inner extends org.example.Other {

    }

    public static org.example.Outer.Inner getInner() {
        throw new Error();
    }
}