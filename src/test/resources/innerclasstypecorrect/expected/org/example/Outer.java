package org.example;

public class Outer {

    public class Inner extends org.example.Other {

    }

    public static Outer.Inner getInner() {
        throw new Error();
    }
}