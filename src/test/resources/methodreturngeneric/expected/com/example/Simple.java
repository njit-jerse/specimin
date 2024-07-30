package com.example;

import com.bar.Bar;
import com.foo.InOtherPackage;

public class Simple {

    public Bar<InOtherPackage> foo() {
        return Bar.getOtherPackage();
    }

    public Bar<InSamePackage> bar() {
        return Bar.getSamePackage();
    }

    public Bar<Integer> baz() {
        return Bar.getJavaLang();
    }
}
