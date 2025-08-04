package com.bar;

public class Bar<T> {

    public static com.bar.Bar<com.foo.InOtherPackage2> getOtherPackage2() {
        throw new java.lang.Error();
    }
}
