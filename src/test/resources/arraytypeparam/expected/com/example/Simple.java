package com.example;

import java.util.List;

class Simple {

    List<Object[]> toHoldTheArgs;

    void bar(Object[] args) {
        toHoldTheArgs = List.<Object[]>of(args);
    }
}
