package org.fortest;

public class UnsolvedType extends Throwable {

    public static int getType() {
        throw new Error();
    }
}
