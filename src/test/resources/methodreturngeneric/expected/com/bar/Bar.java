package com.bar;

public class Bar<T> {

    public static com.bar.Bar<com.foo.InOtherPackage> getOtherPackage() {
        throw new java.lang.Error();
    }

    public static com.bar.Bar<com.example.InSamePackage> getSamePackage() {
        throw new java.lang.Error();
    }

    public static com.bar.Bar<Integer> getJavaLang() {
        throw new java.lang.Error();
    }
}
