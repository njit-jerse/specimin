package com.example;

import com.example.MyClass;
import unreal.pack.AClass;

class Simple {
    // Target method.
    void bar() {
        int x  = 5;
        String y = 7;
        AClass z = new AClass();
        MyClass.process(x);
        org.testing.ThisClass.process(y, z);
    }
}
