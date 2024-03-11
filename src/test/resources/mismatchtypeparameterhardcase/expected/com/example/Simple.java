package com.example;

import org.testing.UnsolvedType;
import java.util.List;

class Simple {

    UnsolvedType unsolvedField;

    int bar(List<String> input) {
        return bar(unsolvedField.getList());
    }
}
