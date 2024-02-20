package org.testing;

public interface D {

    public default int baz() {
        throw new Error();
    }
}
