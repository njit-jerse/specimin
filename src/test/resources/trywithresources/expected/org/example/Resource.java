package org.example;

public class Resource implements AutoCloseable {

    public void close() throws Exception {
        throw new Error();
    }
}
