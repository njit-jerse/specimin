package com.example;

import com.bar.Bar;

public class Simple {

    public Bar<com.foo.InOtherPackage2> alreadyQualified() {
        return Bar.getOtherPackage2();
    }
}
