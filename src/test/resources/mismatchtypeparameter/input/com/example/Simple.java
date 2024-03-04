package com.example;

import org.testing.UnsolvedType;

class Simple {
    UnsolvedType unsolvedField;
    void bar(int input) {
        bar(unsolvedField.getInt());
    }
}
