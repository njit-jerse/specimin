package com.example;

import org.example.Other;
import org.example.Outer;

public class Simple {
    public Other bar() {
        Outer.Inner in = Outer.getInner();
        return in;
    }
}
