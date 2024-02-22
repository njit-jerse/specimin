package com.example;

import java.util.Map;

class Foo {

    Map<Baz, String> field;

    boolean bar() {
        return field.isEmpty();
    }
}
