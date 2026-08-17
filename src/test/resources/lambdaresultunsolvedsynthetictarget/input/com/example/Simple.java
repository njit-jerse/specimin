package com.example;

import java.util.function.Supplier;

import org.example.Foo;
import org.example.Item;

class Simple {
    // Target method.
    void bar(Item item) {
        Supplier<Foo> s = () -> item.get();
    }
}
