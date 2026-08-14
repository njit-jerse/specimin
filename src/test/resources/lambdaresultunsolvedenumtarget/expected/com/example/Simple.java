package com.example;

import java.util.function.Supplier;

import org.example.Item;

class Simple {
    void bar(Item item) {
        Supplier<Color> asColor = () -> item.get();
        Supplier<Payload> asPayload = () -> item.get();
    }
}
