package com.bar;

public class Bar<T> {

    public static com.bar.Bar<com.foo.InOtherPackage> getOtherPackage() {
        throw new Error();
    }

    public static com.bar.Bar<com.example.InSamePackage> getSamePackage() {
        throw new Error();
    }

    public static com.bar.Bar<Integer> getJavaLang() {
        throw new Error();
    }
}
