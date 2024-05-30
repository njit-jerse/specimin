package com.example;

import java.util.ArrayList;
import java.util.List;

class Simple {
    void bar() {
        foo(new ArrayList<?>());
    }

    void foo(List<?> baz) {
    }
}
