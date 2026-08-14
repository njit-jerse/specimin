package com.example;

import java.util.function.Supplier;

import org.example.Item;

class Simple {
    void bar(Item item, boolean flag) {
        Supplier<String> asString = () -> {
            if (flag) {
                return "";
            }
            return item.get();
        };
        Supplier<Payload> asPayload = () -> item.get();
    }
}
