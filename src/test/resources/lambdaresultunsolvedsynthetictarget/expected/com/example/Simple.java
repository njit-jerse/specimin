package com.example;

import java.util.function.Supplier;

import org.example.Foo;
import org.example.Item;

class Simple {
    void bar(Item item) {
        Supplier<Foo> s = () -> item.get();
    }
}
