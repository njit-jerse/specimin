package org.testing;

public interface B {

    public default int baz() {
        throw new java.lang.Error();
    }
}
