package com.example;

import static org.example.TestUtil.when;
import static org.example.TestUtil.mock;

import com.example.Banana;

class Simple {

    void bar() {
        Integer s = mock(2);
        Banana b = mock(3);
        when(5).then("Foo");
        when("bar").then(mock(1));
    }
}
