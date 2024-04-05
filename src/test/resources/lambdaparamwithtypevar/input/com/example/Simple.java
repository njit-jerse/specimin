package com.example;

import org.example.FunctionalProgramming;
import org.example.MyList;
import com.example.myotherpkg.MyOtherObject;

class Simple {
    // Target method.
    void bar() {
        FunctionalProgramming.map((MyList<MyOtherObject> l) -> l.toArray());
    }
}
