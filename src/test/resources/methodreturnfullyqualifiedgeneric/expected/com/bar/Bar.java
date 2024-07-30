package com.bar;

public class Bar<T> {

    public static com.bar.Bar<com.example.InOtherPackage2> getOtherPackage2() {
        throw new Error();
    }
}
