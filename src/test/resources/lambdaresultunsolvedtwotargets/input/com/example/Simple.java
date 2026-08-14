package com.example;

import java.util.function.Supplier;

import org.example.Item;

class Simple {
    // Target method.
    void bar(Item item) {
        Supplier<String> s = () -> { return item.getPayload(); };
        Supplier<Integer> i = () -> { return item.getPayload(); };
    }
}
