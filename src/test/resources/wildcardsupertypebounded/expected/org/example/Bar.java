package org.example;
public class Bar {

    public org.example.Baz<? super org.example.Thing> get2() {
        throw new java.lang.Error();
    }

    public org.example.Baz<? extends org.example.Thing> get1() {
        throw new java.lang.Error();
    }
}
