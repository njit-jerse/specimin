package com.example;

import org.example.FunctionalProgramming.map;
import org.example.MyList;
import com.example.myotherpkg.MyOtherObject;

class Simple {
    // Target method.
    void bar() {
        map((MyList<MyOtherObject> l) -> l.toArray());
    }
}
