package com.example;

import java.util.function.Supplier;

import org.example.Item;

class Simple {
    // Target method.
    void bar(Item item) {
        Supplier<Payload> supplier = () -> item.getPayload();
    }
}
