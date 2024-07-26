package org.example;

public class OtherResource implements java.lang.AutoCloseable {

    public void close() throws java.lang.Exception {
        throw new Error();
    }
}
