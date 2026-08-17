package com.example;

import java.util.function.BiFunction;

import org.example.Item;

class Simple {

    void bar(Item item) {
        apply((key, count) -> { return item.getPayload(); });
    }

    void apply(BiFunction<String, Integer, Payload> f) {
        throw new java.lang.Error();
    }
}
