package com.example;

import sample.pack.MyType;

public class Child extends Parent {

    public MyType returnLocalName() {
        if (3 > 4) {
            MyType thisName = new MyType();
            return thisName;
        } else if (4 > 7) {
            MyType thatName = new MyType();
            return thatName;
        } else {
            return theName;
        }
    }
}
