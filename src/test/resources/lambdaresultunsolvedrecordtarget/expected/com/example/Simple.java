package com.example;

import java.util.function.Supplier;

import org.example.Item;

class Simple {
    void bar(Item item) {
        Supplier<Point> asPoint = () -> item.get();
        Supplier<Payload> asPayload = () -> item.get();
    }
}
