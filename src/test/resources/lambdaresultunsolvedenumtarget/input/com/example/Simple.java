package com.example;

import java.util.function.Supplier;

import org.example.Item;

class Simple {
    // Target method.
    void bar(Item item) {
        Supplier<Color> asColor = () -> item.get();
        Supplier<Payload> asPayload = () -> item.get();
    }
}
