package com.example;

import sample.pack.MyType;

public class Child extends Parent {
    // for SpecSlice, an implicit called superfield and a variable with an unsolved type will both be seen as unsolved NameExpr instances. This test is to make sure that SpecSlice will not confuse between those two cases.
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
