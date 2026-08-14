package com.example;

import java.util.function.Supplier;

import org.example.Item;

class Simple {

    void bar(Item item) {
        Supplier<Payload> p = () -> { return item.getPayload(); };
        Supplier<String> s = () -> { return item.getPayload(); };
    }
}
