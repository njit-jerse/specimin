package com.example;

import static org.example.TestUtil.when;

class Simple {

    void bar() {
        when(5).then("Foo");
        when("bar").then(1);
    }
}
