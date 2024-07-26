package org.example;

public class ThirdResource implements java.lang.AutoCloseable {

    public void close() throws java.lang.Exception {
        throw new Error();
    }
}
