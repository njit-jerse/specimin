package com.example;

import java.util.List;
import org.testing.UnsolvedType;

class Simple {

    UnsolvedType unsolvedField;

    int bar(List<String> input) {
        int number = bar(unsolvedField.getList());
        String aString = unsolvedField.getList().get(unsolvedField.getInt());
        return 0;
    }
}
