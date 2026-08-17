package com.example;

import java.util.function.Supplier;

import org.example.Item;

class Simple {
    // Target method.
    Supplier<Payload> bar(Item item) {
        return () -> { return item.getPayload(); };
    }
}
