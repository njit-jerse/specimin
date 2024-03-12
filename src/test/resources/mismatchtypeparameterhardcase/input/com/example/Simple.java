package com.example;

import org.testing.UnsolvedType;
import java.util.List;

class Simple {
    UnsolvedType unsolvedField;
    int bar(List<String> input) {
        int number = bar(unsolvedField.getList());
        String aString = unsolvedField.getList().get(unsolvedField.getInt());
        return 0;
    }
}
