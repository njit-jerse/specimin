package com.example;

import java.util.function.Supplier;

import org.example.Item;

class Simple {

    Supplier<Payload> bar(Item item) {
        return () -> { return item.getPayload(); };
    }
}
