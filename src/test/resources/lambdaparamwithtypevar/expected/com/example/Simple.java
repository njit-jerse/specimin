package com.example;

import com.example.myotherpkg.MyOtherObject;
import org.example.FunctionalProgramming;
import org.example.MyList;

class Simple {

    void bar() {
        FunctionalProgramming.map((MyList<MyOtherObject> l) -> l.toArray());
    }
}
