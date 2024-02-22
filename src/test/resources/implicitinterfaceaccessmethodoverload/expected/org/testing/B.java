package org.testing;

public interface B {

    public default FooReturnType foo(int parameter0, java.lang.String parameter1) {
        throw new Error();
    }
}
