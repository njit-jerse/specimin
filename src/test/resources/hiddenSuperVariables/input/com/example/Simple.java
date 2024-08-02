package com.example;

import com.library.number.SimpleInt;

public class Simple extends Simplicity{
    // To SpecSlice, both "return myInteger" and "return correct" will be seen as unsolved NameExpr instances. This test make sure that SpecSlice can create appropriate synthetic files for these cases.
    private SimpleInt myInteger = null;
    public SimpleInt getMyInteger() {
        return myInteger;
    }
    public boolean getCorrect() {
        return correct;
    }
}
