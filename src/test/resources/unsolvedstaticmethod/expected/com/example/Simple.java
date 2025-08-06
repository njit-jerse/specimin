package com.example;

import unreal.pack.AClass;

class Simple {

    void bar() {
        int x = 5;
        String y = "hello";
        AClass z = new AClass();
        MyClass.process(x);
        org.testing.ThisClass.process(y, z);
    }
}
