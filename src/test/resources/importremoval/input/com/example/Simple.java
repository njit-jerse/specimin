package com.example;

import java.util.ArrayList;
import java.util.List;

public class Simple {
    // Target method.
    void bar() {
        List l = new ArrayList();
    }

    void baz(Object o) {
        if (o instanceof NonsensicalList) {
            NonsensicalList.getInstance();
        }
    }
}
