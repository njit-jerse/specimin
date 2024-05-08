package com.example;

import org.example.Outer;
import org.example.Other;

public class Simple {
    public Other bar() {
        Outer.Inner in = Outer.getInner();
        return in;
    }
}
