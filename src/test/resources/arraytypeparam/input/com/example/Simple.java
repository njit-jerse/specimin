package com.example;

import java.util.List;

class Simple {

    List<Object[]> toHoldTheArgs;

    // Target method.
    void bar(Object[] args) {
        toHoldTheArgs = List.of(args);
    }
}
